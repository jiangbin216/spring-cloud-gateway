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

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.tuple.Tuple;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * @author Spencer Gibb
 */
public class SetPathGatewayFilterFactory implements GatewayFilterFactory {

	public static final String TEMPLATE_KEY = "template";

	@Override
	public List<String> argNames() {
		return Arrays.asList(TEMPLATE_KEY);
	}

	@Override
	@SuppressWarnings("unchecked")
	public GatewayFilter apply(Tuple args) {
		String template = args.getString(TEMPLATE_KEY);
		UriTemplate uriTemplate = new UriTemplate(template);

		return (exchange, chain) -> {
			PathMatchInfo variables = exchange.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			ServerHttpRequest req = exchange.getRequest();
			addOriginalRequestUrl(exchange, req.getURI());
			Map<String, String> uriVariables;

			if (variables != null) {
				uriVariables = variables.getUriVariables();
			} else {
				uriVariables = Collections.emptyMap();
			}

			// 使用 路径参数进行 替换 请求Path
			URI uri = uriTemplate.expand(uriVariables);
			String newPath = uri.getPath();

			exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, uri);

			ServerHttpRequest request = req.mutate()
					.path(newPath)
					.build();

			return chain.filter(exchange.mutate().request(request).build());
		};
	}
}
