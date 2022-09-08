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
import io.gravitee.gateway.api.http.HttpHeaders;
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OnResponseContent")
@GatewayTest
class XSLTTransformationPolicyResponseIntegrationTest extends AbstractXSLTTransformationPolicyIntegrationTest {

    @Test
    @DisplayName("Should transform and add header")
    @DeployApi("/apis/xslt-response-valid-stylesheet01.json")
    void shouldTransformAndAddHeadersOnResponseContent(WebClient client) throws IOException {
        String file01 = loadResource("/io/gravitee/policy/xslt/file01.xml");
        wiremock.stubFor(post("/endpoint").willReturn(ok(file01)));

        final TestObserver<HttpResponse<Buffer>> obs = client.post("/test").rxSend().test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                String expectedResponse = loadResource("/io/gravitee/policy/xslt/valid-output01.xml");
                assertThat(response.bodyAsString()).isEqualTo(expectedResponse);

                MultiMap headers = response.headers();
                assertThat(headers.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
                assertThat(headers.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_XML);
                assertThat(headers.contains(HttpHeaderNames.TRANSFER_ENCODING)).isFalse();
                assertThat(headers.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
                return true;
            })
            .assertNoErrors();
    }

    @Test
    @DisplayName("Should not transform when TransformationException thrown")
    @DeployApi("/apis/xslt-response-invalid-stylesheet01.json")
    void shouldNotTransformAndAddHeadersOnRequestContent(WebClient client) throws IOException {
        String file01 = loadResource("/io/gravitee/policy/xslt/file01.xml");
        wiremock.stubFor(post("/endpoint").willReturn(ok(file01)));

        final TestObserver<HttpResponse<Buffer>> obs = client.post("/test").rxSend().test();

        awaitTerminalEvent(obs)
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(500);
                assertThat(response.bodyAsString()).contains("Unable to apply XSL Transformation:");
                return true;
            })
            .assertNoErrors();
    }
}
