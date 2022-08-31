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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.MediaType;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.spel.SpelTemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.xslt.configuration.PolicyScope;
import io.gravitee.policy.xslt.configuration.XSLTTransformationPolicyConfiguration;
import io.gravitee.reporter.api.http.Metrics;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.Instant;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class XSLTTransformationPolicyIntegrationTest {

    private XSLTTransformationPolicy xsltTransformationPolicy;

    private XSLTTransformationPolicyConfiguration xsltTransformationPolicyConfiguration;

    @Mock
    protected ExecutionContext executionContext;

    @Mock
    private PolicyChain policyChain;

    @Spy
    private Request request;

    @Spy
    private Response response;

    private TemplateEngine templateEngine;

    @BeforeEach
    public void init() {
        xsltTransformationPolicyConfiguration = new XSLTTransformationPolicyConfiguration();
        xsltTransformationPolicy = new XSLTTransformationPolicy(xsltTransformationPolicyConfiguration);
        templateEngine = mock(SpelTemplateEngine.class);
        when(templateEngine.convert(any())).thenAnswer(returnsFirstArg());
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
    }

    @Test
    @DisplayName("Should transform and add header OnRequestContent")
    void shouldTransformAndAddHeadersOnRequestContent() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet01.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file01.xml");
        String expected = loadResource("/io/gravitee/policy/xslt/output01.xml");

        // Prepare context
        xsltTransformationPolicyConfiguration.setStylesheet(stylesheet);
        xsltTransformationPolicyConfiguration.setScope(PolicyScope.REQUEST);
        when(request.headers()).thenReturn(HttpHeaders.create());

        final ReadWriteStream<Buffer> result = xsltTransformationPolicy.onRequestContent(request, policyChain, executionContext);
        assertThat(result).isNotNull();
        result.bodyHandler(resultBody -> {
            assertResultingJsonObjectsAreEquals(expected, resultBody);
        });

        result.write(Buffer.buffer(xml));
        result.end();

        HttpHeaders headers = request.headers();
        assertThat(headers.contains(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE)).isTrue();
        assertThat(headers.get(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_XML);
        assertThat(headers.contains(io.gravitee.common.http.HttpHeaders.TRANSFER_ENCODING)).isFalse();
        assertThat(headers.contains(io.gravitee.common.http.HttpHeaders.CONTENT_LENGTH)).isTrue();
    }

    @Test
    @DisplayName("Should not transform when TransformationException thrown OnRequestContent")
    void shouldNotTransformAndAddHeadersOnRequestContent() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet_invalid.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file01.xml");

        // Prepare context
        xsltTransformationPolicyConfiguration.setStylesheet(stylesheet);
        xsltTransformationPolicyConfiguration.setScope(PolicyScope.REQUEST);
        when(request.headers()).thenReturn(HttpHeaders.create());
        when(request.metrics()).thenReturn(Metrics.on(Instant.now().toEpochMilli()).build());

        final ReadWriteStream<Buffer> result = xsltTransformationPolicy.onRequestContent(request, policyChain, executionContext);
        assertThat(result).isNotNull();

        result.write(Buffer.buffer(xml));
        result.end();

        HttpHeaders headers = request.headers();
        assertThat(headers.contains(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE)).isFalse();
        assertThat(headers.contains(io.gravitee.common.http.HttpHeaders.TRANSFER_ENCODING)).isFalse();
        assertThat(headers.contains(io.gravitee.common.http.HttpHeaders.CONTENT_LENGTH)).isFalse();
        assertThat(request.metrics().getMessage()).contains("Unable to apply XSL Transformation:");
        verify(policyChain, times(1)).streamFailWith(any());
    }

    @Test
    @DisplayName("Should transform and add header OnResponseContent")
    void shouldTransformAndAddHeadersOnResponseContent() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet01.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file01.xml");
        String expected = loadResource("/io/gravitee/policy/xslt/output01.xml");

        // Prepare context
        xsltTransformationPolicyConfiguration.setStylesheet(stylesheet);
        xsltTransformationPolicyConfiguration.setScope(PolicyScope.RESPONSE);
        when(response.headers()).thenReturn(HttpHeaders.create());

        final ReadWriteStream<Buffer> result = xsltTransformationPolicy.onResponseContent(response, policyChain, executionContext);
        assertThat(result).isNotNull();
        result.bodyHandler(resultBody -> {
            assertResultingJsonObjectsAreEquals(expected, resultBody);
        });

        result.write(Buffer.buffer(xml));
        result.end();

        HttpHeaders headers = response.headers();
        assertThat(headers.contains(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE)).isTrue();
        assertThat(headers.get(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_XML);
        assertThat(headers.contains(io.gravitee.common.http.HttpHeaders.TRANSFER_ENCODING)).isFalse();
        assertThat(headers.contains(io.gravitee.common.http.HttpHeaders.CONTENT_LENGTH)).isTrue();
    }

    @Test
    @DisplayName("Should not transform when TransformationException thrown OnResponseContent")
    void shouldNotTransformAndAddHeadersOnResponseContent() throws Exception {
        String stylesheet = loadResource("/io/gravitee/policy/xslt/stylesheet_invalid.xsl");
        String xml = loadResource("/io/gravitee/policy/xslt/file01.xml");

        // Prepare context
        xsltTransformationPolicyConfiguration.setStylesheet(stylesheet);
        xsltTransformationPolicyConfiguration.setScope(PolicyScope.RESPONSE);
        HttpHeaders httpHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(httpHeaders);

        final ReadWriteStream<Buffer> result = xsltTransformationPolicy.onResponseContent(response, policyChain, executionContext);
        assertThat(result).isNotNull();

        result.write(Buffer.buffer(xml));
        result.end();

        assertThat(response.headers().contains(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE)).isFalse();
        assertThat(response.headers().contains(io.gravitee.common.http.HttpHeaders.TRANSFER_ENCODING)).isFalse();
        assertThat(response.headers().contains(io.gravitee.common.http.HttpHeaders.CONTENT_LENGTH)).isFalse();
        verify(policyChain, times(1)).streamFailWith(any());
    }

    private String loadResource(String resource) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(resource);
        StringWriter sw = new StringWriter();
        IOUtils.copy(is, sw, "UTF-8");
        return sw.toString();
    }

    private void assertResultingJsonObjectsAreEquals(String expected, Object resultBody) {
        Diff diff = DiffBuilder.compare(expected).ignoreWhitespace().withTest(resultBody.toString()).checkForIdentical().build();
        assertThat(diff.hasDifferences()).withFailMessage("XML identical %s", diff.toString()).isFalse();
    }
}
