/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lib.elasticsearch;

import static io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.http.ConnectionClosedException;
import org.apache.http.conn.ConnectTimeoutException;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.transport.Endpoint;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import net.bluemind.configfile.elastic.ElasticsearchConfig.Client;
import net.bluemind.lib.elasticsearch.exception.ElasticRetryException;

public class RetryingRestClientTransport extends RestClientTransport {
	private static Logger logger = LoggerFactory.getLogger(RetryingRestClientTransport.class);

	private static final Set<Integer> retryableStatusCode = Set.of(429, 502, 503, 504);

	private final Retry retry;

	public RetryingRestClientTransport(RestClient restClient, JsonpMapper mapper, Config config) {
		super(restClient, mapper);
		RetryConfig retryConfig = retryConfigOf(config);
		this.retry = Retry.of("Elasticsearch client request retryer", retryConfig);
		retry.getEventPublisher()
				.onRetry(e -> logger.error(
						"[es][retry] Elasticsearch request fails, retrying in {}ms  ({} done /{}): {}",
						e.getWaitInterval().toMillis(), e.getNumberOfRetryAttempts(), retryConfig.getMaxAttempts(),
						e.getLastThrowable().getMessage()));
	}

	@Override
	public <R, S, E> S performRequest(R request, Endpoint<R, S, E> endpoint, TransportOptions options)
			throws IOException {
		try {
			return retry.executeCallable(() -> super.performRequest(request, endpoint, options));
		} catch (IOException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new ElasticRetryException(e);
		}
	}

	private Predicate<Throwable> isRetryableException() {
		return e -> (e instanceof ConnectTimeoutException) || (e instanceof SocketTimeoutException)
				|| (e instanceof ConnectionClosedException) || (e instanceof ConnectException);

	}

	private Predicate<Throwable> isRetryableStatusCode() {
		return e -> isRetryableElasticsearchException(e) || isRetryableResponseException(e);
	}

	private boolean isRetryableElasticsearchException(Throwable e) {
		return e instanceof ElasticsearchException ee && ee.response() != null
				&& retryableStatusCode.contains(ee.response().status());
	}

	private boolean isRetryableResponseException(Throwable e) {
		return e instanceof ResponseException ee && ee.getResponse() != null
				&& retryableStatusCode.contains(ee.getResponse().getStatusLine().getStatusCode());
	}

	private RetryConfig retryConfigOf(Config config) {
		RetryConfig retryConfig;
		try {
			Client.Retry retryParams = Client.Retry.of(config);
			logger.info("[es] Elasticsearch client retry policy: {}", retryParams);
			IntervalFunction interval = ofExponentialBackoff(retryParams.delay(), retryParams.multiplier());
			retryConfig = (!retryParams.enabled()) //
					? noRetryConfig()
					: RetryConfig.custom() //
							.maxAttempts(retryParams.count()) //
							.intervalFunction(interval) //
							.retryOnException(e -> isRetryableException().test(e) || isRetryableStatusCode().test(e)) //
							.failAfterMaxAttempts(false) //
							.build();
		} catch (ConfigException e) {
			retryConfig = noRetryConfig();
			logger.error("[es] Elasticsearch client retry policy has invalid configuration, disabled: {}",
					e.getMessage());
		}
		return retryConfig;
	}

	private RetryConfig noRetryConfig() {
		return RetryConfig.custom().maxAttempts(1).build();
	}

}
