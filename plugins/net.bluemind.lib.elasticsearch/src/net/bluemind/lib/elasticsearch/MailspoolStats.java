package net.bluemind.lib.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation.Builder.ContainerBuilder;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregationSource;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.NamedValue;
import net.bluemind.lib.elasticsearch.MailspoolStats.FolderCount.SampleStrategy;

public class MailspoolStats {
	private static final Logger logger = LoggerFactory.getLogger(MailspoolStats.class);

	private final ElasticsearchClient esClient;

	public MailspoolStats(ElasticsearchClient esClient) {
		this.esClient = esClient;
	}

	public boolean exists(String mailboxUid) throws ElasticsearchException, IOException {
		return esClient.indices().existsAlias(e -> e.name(getMailboxAlias(mailboxUid))).value();
	}

	public static record FolderCount(String folderUid, long count) {

		public enum SampleStrategy {
			RANDOM, BIGGEST;

			public static Optional<SampleStrategy> valueOfCaseInsensitive(String name) {
				try {
					return Optional.of(valueOf(name.toUpperCase()));
				} catch (IllegalArgumentException e) {
					return Optional.empty();
				}
			}
		}

		public static record Parameters(boolean allFolders, SampleStrategy sampleStrategy, int sampleSize,
				boolean emptyFolder, boolean includeDeleted) {

		}
	}

	public List<FolderCount> countByFolders(String mailboxUid, FolderCount.Parameters parameters)
			throws ElasticsearchException, IOException {
		Query query = (parameters.includeDeleted) //
				? MatchAllQuery.of(q -> q)._toQuery() //
				: BoolQuery.of(b -> b.mustNot(mn -> mn.term(t -> t.field("is").value("deleted"))))._toQuery();
		return (parameters.allFolders()) //
				? countAllFolders(mailboxUid, parameters.sampleSize(), query) //
				: countSampleFolders(mailboxUid, parameters, query);
	}

	public List<FolderCount> countAllFolders(String mailboxUid, int pageSize, Query query)
			throws ElasticsearchException, IOException {
		String alias = getMailboxAlias(mailboxUid);
		Map<String, CompositeAggregationSource> aggSource = Map.of("in",
				CompositeAggregationSource.of(s -> s.terms(t -> t.field("in"))));

		List<FolderCount> allFolders = new ArrayList<>();
		Map<String, FieldValue> afterKey = null;
		do {
			Map<String, FieldValue> afterKeyCopy = (afterKey == null) ? null : new HashMap<>(afterKey);
			SearchResponse<Void> response = esClient.search(s -> s //
					.size(0) //
					.index(alias) //
					.query(query) //
					.aggregations("all", a -> a.composite(c -> {
						c.sources(Arrays.asList(aggSource)).size(pageSize);
						if (afterKeyCopy != null && !afterKeyCopy.isEmpty()) {
							c.after(afterKeyCopy);
						}
						return c;
					})), Void.class);
			List<FolderCount> foldersCount = response.aggregations().get("all").composite().buckets().array().stream()
					.map(bucket -> new FolderCount(bucket.key().get("in").stringValue(), bucket.docCount())).toList();
			allFolders.addAll(foldersCount);
			afterKey = (foldersCount.size() < pageSize) //
					? Collections.emptyMap() //
					: response.aggregations().get("all").composite().afterKey();
		} while (!afterKey.isEmpty());
		return allFolders;

	}

	private List<FolderCount> countSampleFolders(String mailboxUid, FolderCount.Parameters parameters, Query query)
			throws ElasticsearchException, IOException {
		String alias = getMailboxAlias(mailboxUid);
		int minDocCount = (parameters.emptyFolder) ? 0 : 1;
		SearchResponse<Void> response = esClient.search(s -> s //
				.size(0) //
				.index(alias) //
				.query(query) //
				.aggregations("in", a -> {
					ContainerBuilder terms = a.terms(t -> { //
						t.field("in").size(parameters.sampleSize()).minDocCount(minDocCount);
						if (SampleStrategy.RANDOM.equals(parameters.sampleStrategy)) {
							t.order(NamedValue.of("sample>random.max", SortOrder.Asc));
						}
						return t;
					});
					if (SampleStrategy.RANDOM.equals(parameters.sampleStrategy)) {
						terms.aggregations("sample", a2 -> a2 //
								.sampler(spl -> spl.shardSize(1)) //
								.aggregations("random", a3 -> a3 //
										.stats(st -> st.script(v -> v.inline(i -> i.source("Math.random()"))))));
					}
					return terms;
				}), Void.class);

		return response.aggregations().get("in").sterms().buckets().array().stream()
				.map(bucket -> new FolderCount(bucket.key().stringValue(), bucket.docCount())).toList();
	}

	public long missingParentCount(String mailboxUid) throws ElasticsearchException, IOException {
		String alias = getMailboxAlias(mailboxUid);
		SearchResponse<Void> response = esClient.search(s -> s //
				.size(0) //
				.index(alias) //
				.trackTotalHits(t -> t.enabled(true)) //
				.query(q -> q.bool(b -> b //
						.mustNot(mn -> mn.hasParent(p -> p //
								.parentType("body") //
								.query(q2 -> q2.matchAll(a -> a)) //
								.score(false))) //
						.mustNot(mn -> mn.term(t -> t.field("body_msg_link").value("body"))))),
				Void.class);

		return response.hits().total().value();
	}

	private String getMailboxAlias(String mailboxId) {
		return "mailspool_alias_" + mailboxId;
	}

}
