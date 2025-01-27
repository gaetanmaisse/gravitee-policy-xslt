/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.xslt;

import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.stream.TransformableRequestStreamBuilder;
import io.gravitee.gateway.api.http.stream.TransformableResponseStreamBuilder;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponseContent;
import io.gravitee.policy.xslt.configuration.PolicyScope;
import io.gravitee.policy.xslt.configuration.XSLTTransformationPolicyConfiguration;
import io.gravitee.policy.xslt.transformer.TransformerFactory;
import io.gravitee.policy.xslt.utils.SAXSourceUtil;
import java.io.ByteArrayOutputStream;
import java.util.function.Function;
import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class XSLTTransformationPolicy {

    /**
     * XSLT transformation configuration
     */
    private final XSLTTransformationPolicyConfiguration xsltTransformationPolicyConfiguration;

    private static final String XSLT_ENV_VAR_SECURE_PROCESSING = "policy.xslt.secure-processing";
    public static final String SECURE_PROCESSING_DEFAULT_VALUE = "true";

    public XSLTTransformationPolicy(final XSLTTransformationPolicyConfiguration xsltTransformationPolicyConfiguration) {
        this.xsltTransformationPolicyConfiguration = xsltTransformationPolicyConfiguration;
    }

    @OnResponseContent
    public ReadWriteStream onResponseContent(Response response, PolicyChain chain, ExecutionContext executionContext) {
        if (
            xsltTransformationPolicyConfiguration.getScope() == null ||
            xsltTransformationPolicyConfiguration.getScope() == PolicyScope.RESPONSE
        ) {
            return TransformableResponseStreamBuilder
                .on(response)
                .chain(chain)
                .contentType(MediaType.APPLICATION_XML)
                .transform(toXSLT(executionContext))
                .build();
        }

        return null;
    }

    @OnRequestContent
    public ReadWriteStream onRequestContent(Request request, PolicyChain chain, ExecutionContext executionContext) {
        if (xsltTransformationPolicyConfiguration.getScope() == PolicyScope.REQUEST) {
            return TransformableRequestStreamBuilder
                .on(request)
                .chain(chain)
                .contentType(MediaType.APPLICATION_XML)
                .transform(toXSLT(executionContext))
                .build();
        }

        return null;
    }

    public Function<Buffer, Buffer> toXSLT(ExecutionContext executionContext) {
        Environment environment = executionContext.getComponent(Environment.class);
        boolean secureProcessing = Boolean.parseBoolean(
            environment.getProperty(XSLT_ENV_VAR_SECURE_PROCESSING, SECURE_PROCESSING_DEFAULT_VALUE)
        );

        return input -> {
            try {
                // Get XSL stylesheet and transform it using internal template engine
                String stylesheet = executionContext.getTemplateEngine().convert(xsltTransformationPolicyConfiguration.getStylesheet());

                Templates template = TransformerFactory.getInstance().setSecureProcessing(secureProcessing).getTemplate(stylesheet);

                SAXSource saxSource = SAXSourceUtil.createSAXSource(input, secureProcessing);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Result result = new StreamResult(baos);
                Transformer transformer = template.newTransformer();

                // Add parameters
                if (xsltTransformationPolicyConfiguration.getParameters() != null) {
                    xsltTransformationPolicyConfiguration
                        .getParameters()
                        .forEach(parameter -> {
                            if (parameter.getName() != null && !parameter.getName().trim().isEmpty()) {
                                // Apply SpEL conversion
                                String value = (parameter.getValue() != null)
                                    ? executionContext.getTemplateEngine().getValue(parameter.getValue(), String.class)
                                    : null;

                                transformer.setParameter(parameter.getName(), value);
                            }
                        });
                }

                transformer.transform(saxSource, result);
                return Buffer.buffer(baos.toString());
            } catch (Exception ex) {
                throw new TransformationException("Unable to apply XSL Transformation: " + ex.getMessage(), ex);
            }
        };
    }
}
