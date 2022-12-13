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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

import io.gravitee.el.TemplateEngine;
import io.gravitee.el.spel.SpelTemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.policy.xslt.configuration.XSLTTransformationPolicyConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class XSLTTransformationPolicyUnsecureProcessingTest {

    private XSLTTransformationPolicy xsltTransformationPolicy;

    private XSLTTransformationPolicyConfiguration xsltTransformationPolicyConfiguration;

    @Mock
    protected ExecutionContext executionContext;

    private final MockEnvironment environment = new MockEnvironment();

    @BeforeEach
    public void init() {
        xsltTransformationPolicyConfiguration = new XSLTTransformationPolicyConfiguration();
        xsltTransformationPolicy = new XSLTTransformationPolicy(xsltTransformationPolicyConfiguration);

        TemplateEngine templateEngine = mock(SpelTemplateEngine.class);
        when(templateEngine.convert(any())).thenAnswer(returnsFirstArg());
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);

        when(executionContext.getComponent(Environment.class)).thenReturn(environment);
        environment.setProperty("policy.xslt.secure-processing", "false");
    }

    @Test
    @DisplayName("Should remove external entity injection")
    void shouldRemoveExternalEntityInjection() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet01.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file02.xml");

        xsltTransformationPolicyConfiguration.setStylesheet(stylesheet);

        Buffer result = xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml));
        assertThat(result)
            .hasToString(
                "<!DOCTYPE HTML><html>\n" +
                "   <body>\n" +
                "      <h2>My CD Collection</h2>\n" +
                "      <table border=\"1\">\n" +
                "         <tr bgcolor=\"#9acd32\">\n" +
                "            <th style=\"text-align:left\">Title</th>\n" +
                "            <th style=\"text-align:left\">Artist</th>\n" +
                "         </tr>\n" +
                "      </table>\n" +
                "   </body>\n" +
                "</html>"
            );
    }

    @Test
    @DisplayName("Should process stylesheet containing access to filesystem")
    void shouldProcessStylesheetThatAccessesFilesystem() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet_filesystem_access2.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file01.xml");

        xsltTransformationPolicyConfiguration.setStylesheet(stylesheet);

        Buffer result = xsltTransformationPolicy.toXSLT(executionContext).apply(Buffer.buffer(xml));
        assertThat(result).isNotNull();
    }

    private String loadResource(String resource) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(resource);
        StringWriter sw = new StringWriter();
        IOUtils.copy(is, sw, "UTF-8");
        return sw.toString();
    }
}
