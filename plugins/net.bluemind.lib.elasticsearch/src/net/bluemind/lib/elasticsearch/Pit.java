/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

public class Pit<T> implements AutoCloseable {

	public static final int DEFAULT_PAGE_SIZE = 1000;

	public interface PaginableSearchQueryBuilder extends Function<SearchRequest.Builder, SearchRequest.Builder> {

	}

	public record PaginationParams(int from, int size, SortOptions sort, int pageSize) {

		public PaginationParams(int from, int size, SortOptions sort) {
			this(from, size, sort, DEFAULT_PAGE_SIZE);
		}

		public static PaginationParams all(SortOptions sort, int pageSize) {
			return new PaginationParams(0, -1, sort, pageSize);
		}

		public static PaginationParams all(SortOptions sort) {
			return new PaginationParams(0, -1, sort, DEFAULT_PAGE_SIZE);
		}
	}

	private static final BiPredicate<Integer, Integer> continueSearch = (querySize, size) -> querySize == -1
			|| (querySize > 0 && size < querySize);
	private static final BiPredicate<PaginationParams, Integer> hitInRange = (params, pos) -> pos >= params.from
			&& (params.size == -1 || (params.size > 0 && pos < (params.from + params.size)));

	public final String id;
	private final ElasticsearchClient esClient;
	private final long budget;
	private final Class<T> hitClass;
	private List<FieldValue> sortFields;
	private boolean hasNext;
	private long start;
	private boolean invalidated;

	private static final Logger logger = LoggerFactory.getLogger(Pit.class);

	private Pit(String id, ElasticsearchClient esClient, long budget, Class<T> hitClass) {
		this.id = id;
		this.esClient = esClient;
		this.start = System.nanoTime();
		this.budget = budget;
		this.invalidated = false;
		this.hitClass = hitClass;
	}

	public static <T> Pit<T> allocate(ElasticsearchClient esClient, String index, int keepAliveInSeconds,
			Class<T> hitClass) throws ElasticsearchException, IOException {
		return Pit.allocateUsingTimebudget(esClient, index, keepAliveInSeconds, -1, hitClass);
	}

	public static <T> Pit<T> allocateUsingTimebudget(ElasticsearchClient esClient, String index, int keepAliveInSeconds,
			long budget, Class<T> hitClass) throws ElasticsearchException, IOException {
		String pitId = esClient.openPointInTime(pit -> pit //
				.index(index) //
				.keepAlive(t -> t.time(keepAliveInSeconds + "s"))) //
				.id();
		return new Pit<>(pitId, esClient, budget, hitClass);
	}

	@Override
	public void close() throws ElasticsearchException, IOException {
		esClient.closePointInTime(pit -> pit.id(id));
	}

	public boolean hasNext() {
		return !invalidated && hasNext;
	}

	public void consumeHit(Hit<T> h) {
		hasNext = true;
		sortFields = h.sort();
		if (budget != -1 && System.nanoTime() - start > budget) {
			logger.warn("Stopped processing search results as timebudget ({} ns) is exhausted", budget);
			invalidated = true;
		}
	}

	public <U> List<U> allPages(PaginableSearchQueryBuilder paginableSearch, PaginationParams params,
			Function<Hit<T>, U> mapper) throws ElasticsearchException, IOException {
		return allPages(paginableSearch, params, mapper, null);
	}

	public <U> List<U> allPages(PaginableSearchQueryBuilder paginableSearch, PaginationParams params,
			Function<Hit<T>, U> mapper, AtomicLong total) throws ElasticsearchException, IOException {
		AtomicInteger position = new AtomicInteger(0);
		if (total != null) {
			total.set(0L);
		}
		List<U> results = new ArrayList<>();
		do {
			SearchRequest search = adaptSearch(paginableSearch, params.pageSize, params.sort);
			SearchResponse<T> response = esClient.search(search, hitClass);
			if (response.hits() != null && response.hits().hits() != null) {
				response.hits().hits().stream().forEach(hit -> {
					if (hitInRange.test(params, position.get())) {
						results.add(mapper.apply(hit));
					}
					consumeHit(hit);
					position.incrementAndGet();
				});
				if (total != null) {
					total.set(response.hits().total().value());
				}
			}
		} while (hasNext() && continueSearch.test(params.size, results.size()));
		return results;
	}

	public SearchRequest adaptSearch(PaginableSearchQueryBuilder paginableSearch) {
		return adaptSearch(paginableSearch, DEFAULT_PAGE_SIZE, null);
	}

	public SearchRequest adaptSearch(PaginableSearchQueryBuilder paginableSearch, int pageSize, SortOptions sort) {
		hasNext = false;
		return paginableSearch.andThen(s -> {
			s.pit(p -> p.id(id)).size(pageSize);
			if (sort != null) {
				s.sort(sort);
			}
			return (sortFields != null) //
					? s.searchAfter(sortFields).trackTotalHits(t -> t.enabled(true)) //
					: s.trackTotalHits(t -> t.enabled(true));
		}).apply(new SearchRequest.Builder()).build();
	}

}
