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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.spel.SpelTemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.policy.xslt.configuration.XSLTParameter;
import io.gravitee.policy.xslt.configuration.XSLTTransformationPolicyConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class XSLTTransformationPolicyTest {

    private XSLTTransformationPolicy xsltTransformationPolicy;

    @Mock
    private XSLTTransformationPolicyConfiguration xsltTransformationPolicyConfiguration;

    @Mock
    protected ExecutionContext executionContext;

    private TemplateEngine templateEngine;

    @BeforeEach
    public void init() {
        xsltTransformationPolicy = new XSLTTransformationPolicy(xsltTransformationPolicyConfiguration);
        templateEngine = mock(SpelTemplateEngine.class);
        when(templateEngine.convert(any())).thenAnswer(returnsFirstArg());
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
    }

    @Test
    @DisplayName("Should transform input")
    public void shouldTransformInput() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet01.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file01.xml");
        String expected = loadResource("/io/gravitee/policy/xslt/output01.xml");

        // Prepare context
        when(xsltTransformationPolicyConfiguration.getStylesheet()).thenReturn(stylesheet);

        Buffer ret = xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml));
        assertThat(ret).isNotNull();

        Diff diff = DiffBuilder.compare(expected).ignoreWhitespace().withTest(ret.toString()).checkForIdentical().build();
        assertThat(diff.hasDifferences()).withFailMessage("XML identical %s", diff.toString()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when stylesheet is invalid")
    public void shouldThrowExceptionForInvalidStylesheet() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet_invalid.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file01.xml");

        // Prepare context
        when(xsltTransformationPolicyConfiguration.getStylesheet()).thenReturn(stylesheet);

        Assertions.assertThrows(
            TransformationException.class,
            () -> xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml))
        );
    }

    @Test
    @DisplayName("Should throw exception when external entity injection")
    public void shouldThrowExceptionForExternalEntityInjection() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet01.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file02.xml");

        // Prepare context
        when(xsltTransformationPolicyConfiguration.getStylesheet()).thenReturn(stylesheet);

        Assertions.assertThrows(
            TransformationException.class,
            () -> xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml))
        );
    }

    @Test
    @DisplayName("Should not cache SPEL evaluation result")
    void shouldNotCacheSPELEvaluationResult() throws IOException {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet03.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file03.xml");

        XSLTParameter parameter = new XSLTParameter();
        parameter.setName("p");
        parameter.setValue("{#request.headers['test'][0]}");
        // Prepare context
        when(xsltTransformationPolicyConfiguration.getStylesheet()).thenReturn(stylesheet);
        when(xsltTransformationPolicyConfiguration.getParameters()).thenReturn(singletonList(parameter));

        when(templateEngine.getValue("{#request.headers['test'][0]}", String.class)).thenReturn("1");

        xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml));
        xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml));

        verify(templateEngine, times(2)).getValue(argThat(value -> value.equals("{#request.headers['test'][0]}")), eq(String.class));
    }

    @Test
    @DisplayName("Should throw exception when stylesheet contains access to filesystem")
    public void shouldThrowExceptionForStylesheetThatAccessesFilesystem() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet_filesystem_access.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file01.xml");

        when(xsltTransformationPolicyConfiguration.getStylesheet()).thenReturn(stylesheet);

        Assertions.assertThrows(
            TransformationException.class,
            () -> xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml))
        );
    }

    private String loadResource(String resource) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(resource);
        StringWriter sw = new StringWriter();
        IOUtils.copy(is, sw, "UTF-8");
        return sw.toString();
    }
}
