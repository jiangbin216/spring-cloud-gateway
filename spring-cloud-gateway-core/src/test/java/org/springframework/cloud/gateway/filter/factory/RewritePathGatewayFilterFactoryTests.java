/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.gateway.filter.factory;

import java.net.URI;
import java.util.LinkedHashSet;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory.REGEXP_KEY;
import static org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory.REPLACEMENT_KEY;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.tuple.TupleBuilder.tuple;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

/**
 * @author Spencer Gibb
 */
public class RewritePathGatewayFilterFactoryTests {

	@Test
	public void rewritePathFilterWorks() {
		testRewriteFilter("/foo", "/baz", "/foo/bar", "/baz/bar");
	}

	@Test
	public void rewritePathFilterWithNamedGroupWorks() {
		testRewriteFilter("/foo/(?<id>\\d.*)", "/bar/baz/$\\{id}", "/foo/123", "/bar/baz/123");
	}

	private void testRewriteFilter(String regex, String replacement, String actualPath, String expectedPath) {
		GatewayFilter filter = new RewritePathGatewayFilterFactory().apply(tuple().of(REGEXP_KEY, regex, REPLACEMENT_KEY, replacement));

		MockServerHttpRequest request = MockServerHttpRequest
				.get("http://localhost"+ actualPath)
				.build();

		ServerWebExchange exchange = MockServerWebExchange.from(request);

		GatewayFilterChain filterChain = mock(GatewayFilterChain.class);

		ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
		when(filterChain.filter(captor.capture())).thenReturn(Mono.empty());

		filter.filter(exchange, filterChain);

		ServerWebExchange webExchange = captor.getValue();

		assertThat(webExchange.getRequest().getURI()).hasPath(expectedPath);

		URI requestUrl = webExchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
		assertThat(requestUrl).hasScheme("http").hasHost("localhost").hasNoPort().hasPath(expectedPath);
		LinkedHashSet<URI> uris = webExchange.getRequiredAttribute(GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
		assertThat(uris).contains(request.getURI());
	}
}
