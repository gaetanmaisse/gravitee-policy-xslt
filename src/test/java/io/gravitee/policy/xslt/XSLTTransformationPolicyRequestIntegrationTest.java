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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OnRequestContent")
@GatewayTest
class XSLTTransformationPolicyRequestIntegrationTest extends AbstractXSLTTransformationPolicyIntegrationTest {

    @Test
    @DisplayName("Should transform and add header")
    @DeployApi("/apis/xslt-request-valid-stylesheet01.json")
    void shouldTransformAndAddHeadersOnRequestContent(WebClient client) throws IOException {
        wiremock.stubFor(post("/endpoint").willReturn(ok()));

        String file01 = loadResource("/io/gravitee/policy/xslt/file01.xml");
        final TestObserver<HttpResponse<Buffer>> obs = client.post("/test").rxSendBuffer(Buffer.buffer(file01)).test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return true;
            })
            .assertNoErrors();

        String expectedResponse = loadResource("/io/gravitee/policy/xslt/valid-output01.xml");
        wiremock.verify(
            postRequestedFor(urlPathEqualTo("/endpoint"))
                .withHeader(HttpHeaderNames.CONTENT_TYPE, new EqualToPattern(MediaType.APPLICATION_XML))
                .withHeader(HttpHeaderNames.CONTENT_LENGTH, new EqualToPattern("2790"))
                .withoutHeader(HttpHeaderNames.TRANSFER_ENCODING)
                .withRequestBody(equalTo(expectedResponse))
        );
    }

    @Test
    @DisplayName("Should transform with parameters")
    @DeployApi("/apis/xslt-request-valid-stylesheet02.json")
    void shouldTransformWithParametersOnRequestContent(WebClient client) throws IOException {
        wiremock.stubFor(post("/endpoint").willReturn(ok()));

        String file01 = loadResource("/io/gravitee/policy/xslt/file01.xml");
        TestObserver<HttpResponse<Buffer>> obs = client
            .post("/test")
            .putHeader("header-name", "Header Value")
            .rxSendBuffer(Buffer.buffer(file01))
            .test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return true;
            })
            .assertNoErrors();

        String expectedResponse = loadResource("/io/gravitee/policy/xslt/valid-output02.xml");
        wiremock.verify(
            postRequestedFor(urlPathEqualTo("/endpoint"))
                .withHeader(HttpHeaderNames.CONTENT_TYPE, new EqualToPattern(MediaType.APPLICATION_XML))
                .withHeader(HttpHeaderNames.CONTENT_LENGTH, new EqualToPattern("63"))
                .withoutHeader(HttpHeaderNames.TRANSFER_ENCODING)
                .withRequestBody(equalTo(expectedResponse))
        );
        wiremock.resetRequests();

        obs = client.post("/test").putHeader("header-name", "Another Value").rxSendBuffer(Buffer.buffer(file01)).test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return true;
            })
            .assertNoErrors();

        wiremock.verify(postRequestedFor(urlPathEqualTo("/endpoint")).withRequestBody(containing("Another Value")));
        wiremock.resetRequests();
    }

    @Test
    @DisplayName("Should not transform when TransformationException thrown")
    @DeployApi("/apis/xslt-request-invalid-stylesheet01.json")
    void shouldNotTransformAndAddHeadersOnRequestContent(WebClient client) throws IOException {
        wiremock.stubFor(post("/endpoint").willReturn(ok()));

        String file01 = loadResource("/io/gravitee/policy/xslt/file01.xml");
        final TestObserver<HttpResponse<Buffer>> obs = client.post("/test").rxSendBuffer(Buffer.buffer(file01)).test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(500);
                assertThat(response.bodyAsString()).contains("Unable to apply XSL Transformation:");
                return true;
            })
            .assertNoErrors();

        wiremock.verify(0, postRequestedFor(urlPathEqualTo("/endpoint")));
    }
}
