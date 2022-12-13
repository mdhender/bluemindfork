package net.bluemind.lib.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregation;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.elasticsearch.MailspoolStats.FolderCount.SampleStrategy;

public class MailspoolStats {
	private static final Logger logger = LoggerFactory.getLogger(MailspoolStats.class);

	private final Client client;

	public MailspoolStats(Client client) {
		this.client = client;
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

	public Optional<List<FolderCount>> countByFolders(String mailboxUid, FolderCount.Parameters parameters) {
		QueryBuilder query = (parameters.includeDeleted) //
				? QueryBuilders.matchAllQuery()
				: QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("is", "deleted"));
		try {
			return Optional.of((parameters.allFolders()) //
					? countAllFolders(mailboxUid, parameters, query) //
					: countSampleFolders(mailboxUid, parameters, query));
		} catch (InterruptedException | ExecutionException e) {
			logger.error("Unable to get the folder count for {}", mailboxUid, e);
			return Optional.empty();
		}
	}

	private List<FolderCount> countAllFolders(String mailboxUid, FolderCount.Parameters parameters, QueryBuilder query)
			throws InterruptedException, ExecutionException {
		String alias = getMailboxAlias(mailboxUid);
		CompositeAggregationBuilder agg = AggregationBuilders
				.composite("all", Arrays.asList(new TermsValuesSourceBuilder("in").field("in")))
				.size(parameters.sampleSize());
		List<FolderCount> allFolders = new ArrayList<>();
		Map<String, Object> afterKey = null;
		while (afterKey == null || !afterKey.isEmpty()) {
			if (afterKey != null) {
				agg.aggregateAfter(afterKey);
			}
			SearchResponse response = client.prepareSearch(alias) //
					.setSize(0) //
					.setQuery(query) //
					.addAggregation(agg) //
					.execute().get();

			List<FolderCount> foldersCount = response.getAggregations().<CompositeAggregation>get("all").getBuckets()
					.stream() //
					.map(bucket -> new FolderCount((String) bucket.getKey().get("in"), bucket.getDocCount())).toList();

			allFolders.addAll(foldersCount);
			afterKey = (foldersCount.size() < parameters.sampleSize()) //
					? Collections.emptyMap() //
					: response.getAggregations().<CompositeAggregation>get("all").afterKey();
		}
		return allFolders;
	}

	private List<FolderCount> countSampleFolders(String mailboxUid, FolderCount.Parameters parameters,
			QueryBuilder query) throws InterruptedException, ExecutionException {
		String alias = getMailboxAlias(mailboxUid);
		int minDocCount = (parameters.emptyFolder) ? 0 : 1;
		TermsAggregationBuilder agg = AggregationBuilders //
				.terms("in") //
				.field("in") //
				.size(parameters.sampleSize()) //
				.minDocCount(minDocCount);
		if (SampleStrategy.RANDOM.equals(parameters.sampleStrategy)) {
			agg //
					.order(BucketOrder.aggregation("sample>random", "max", false)) //
					.subAggregation(AggregationBuilders.sampler("sample").shardSize(1) //
							.subAggregation(AggregationBuilders.stats("random").script(new Script("Math.random()"))));
		}

		SearchResponse response = client.prepareSearch(alias) //
				.setSize(0) //
				.setQuery(query) //
				.addAggregation(agg) //
				.execute().get();

		return response.getAggregations().<Terms>get("in").getBuckets().stream() //
				.map(bucket -> new FolderCount(bucket.getKeyAsString(), bucket.getDocCount())) //
				.toList();
	}

	public long missingParentCount(String mailboxUid) {
		String alias = getMailboxAlias(mailboxUid);
		BoolQueryBuilder query = QueryBuilders.boolQuery()
				.mustNot(JoinQueryBuilders.hasParentQuery("body", QueryBuilders.matchAllQuery(), false))
				.mustNot(QueryBuilders.termQuery("body_msg_link", "body"));
		TermsAggregationBuilder agg = AggregationBuilders.terms("in").field("in").minDocCount(0);

		try {
			SearchResponse response = client.prepareSearch(alias) //
					.setSize(0) //
					.setTrackTotalHits(true).setQuery(query) //
					.addAggregation(agg) //
					.execute().get();

			return response.getHits().getTotalHits().value;
		} catch (InterruptedException | ExecutionException e) {
			logger.error("Unable to get the missing parent count for {}", mailboxUid, e);
			return 0l;
		}
	}

	private String getMailboxAlias(String mailboxId) {
		return "mailspool_alias_" + mailboxId;
	}

}
