package net.bluemind.index.mail;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.index.reindex.ReindexAction;
import org.elasticsearch.index.reindex.ReindexRequestBuilder;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.InternalSum;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
import org.elasticsearch.search.builder.PointInTimeBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.netflix.spectator.api.Registry;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.MessageSearchResult;
import net.bluemind.backend.mail.api.MessageSearchResult.Mbox;
import net.bluemind.backend.mail.api.SearchQuery;
import net.bluemind.backend.mail.api.SearchQuery.LogicalOperator;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.indexing.IDRange;
import net.bluemind.backend.mail.replica.indexing.IDSet;
import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.backend.mail.replica.indexing.MailSummary;
import net.bluemind.backend.mail.replica.indexing.MessageFlagsHelper;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.Pit;
import net.bluemind.lib.elasticsearch.Queries;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.mailbox.api.ShardStats.MailboxStats;
import net.bluemind.mailbox.api.SimpleShardStats;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.utils.EmailAddress;

public class MailIndexService implements IMailIndexService {
	public static final int SIZE = 200;

	private static final Logger logger = LoggerFactory.getLogger(MailIndexService.class);
	private static final String PENDING_TYPE = "eml";
	static final String MAILSPOOL_TYPE = "recordOrBody";
	public static final String JOIN_FIELD = "body_msg_link";
	public static final String PARENT_TYPE = "body";
	public static final String CHILD_TYPE = "record";
	public static final String INDEX_PENDING = "mailspool_pending";
	public static final String INDEX_PENDING_ALIAS = "mailspool_pending_alias";

	private Registry metricRegistry;
	private IdFactory idFactory;

	public String getIndexAliasName(String entityId) {
		return "mailspool_alias_" + entityId;
	}

	public MailIndexService() {

		metricRegistry = MetricsRegistry.get();
		idFactory = new IdFactory("mailindex-service", metricRegistry, MailIndexService.class);

		VertxPlatform.executeBlockingPeriodic(TimeUnit.HOURS.toMillis(1), i -> getStats());
	}

	@Override
	public Map<String, Object> storeBody(IndexedMessageBody body) {
		logger.debug("Saving body {} to pending index", body);
		Client client = getIndexClient();
		Map<String, Object> content = new HashMap<>();
		content.put("content", body.content);
		content.put("messageId", body.messageId.toString());
		content.put("references", body.references.stream().map(Object::toString).collect(Collectors.toList()));
		content.put("preview", body.preview);
		content.put("subject", body.subject.toString());
		content.put("subject_kw", body.subject.toString());
		content.put("headers", body.headers());
		content.putAll(body.data);
		client.prepareIndex(INDEX_PENDING_ALIAS, PENDING_TYPE).setId(body.uid).setSource(content).execute().actionGet();
		return content;
	}

	private List<String> filterMailspoolIndexNames(GetIndexResponse indexResponse) {
		return Arrays.asList(indexResponse.indices()).stream().filter(i -> !i.startsWith(INDEX_PENDING))
				.collect(Collectors.toList());
	}

	@Override
	public void deleteBodyEntries(List<String> bodyIds) {
		Client client = getIndexClient();
		deleteBodiesFromIndex(bodyIds, INDEX_PENDING_ALIAS, PENDING_TYPE);
		GetIndexResponse resp = client.admin().indices().prepareGetIndex().addIndices("mailspool*").get();
		List<String> shards = filterMailspoolIndexNames(resp);
		for (String index : shards) {
			deleteBodiesFromIndex(bodyIds, index, MAILSPOOL_TYPE);
		}
	}

	private void deleteBodiesFromIndex(List<String> deletedOrphanBodies, String index, String type) {

		QueryBuilder termQuery = QueryBuilders.idsQuery().addIds(deletedOrphanBodies.toArray(new String[0]));
		QueryBuilder queryBuilder = QueryBuilders.constantScoreQuery(termQuery);

		DeleteByQueryRequestBuilder req = new DeleteByQueryRequestBuilder(getIndexClient(),
				DeleteByQueryAction.INSTANCE).abortOnVersionConflict(false);
		req.source().setIndices(index).setTypes(type).setQuery(queryBuilder).get();
	}

	private static class EsBulk implements BulkOperation {

		private BulkRequestBuilder bulk;

		public EsBulk(BulkRequestBuilder bulk) {
			this.bulk = bulk;
		}

		@Override
		public void commit(boolean waitForRefresh) {
			if (waitForRefresh) {
				bulk.setRefreshPolicy(RefreshPolicy.WAIT_UNTIL);
			}
			int actions = bulk.numberOfActions();
			if (actions == 0) {
				logger.warn("Empty bulk, not running.");
			} else {
				bulk.execute().actionGet();
			}
		}

	}

	public BulkOperation startBulk() {
		Client client = getIndexClient();
		return new EsBulk(client.prepareBulk());
	}

	@Override
	public void storeMessage(String mailboxUniqueId, ItemValue<MailboxRecord> item, String user,
			Optional<BulkOperation> bulk) {

		MailboxRecord mail = item.value;
		String parentUid = mail.messageBody;
		logger.debug("Indexing message in mailbox {} using parent uid {}", mailboxUniqueId, parentUid);

		String id = mailboxUniqueId + ":" + item.internalId;

		Client client = getIndexClient();
		String userAlias = getIndexAliasName(user);
		Set<String> is = MessageFlagsHelper.asFlags(mail.flags);

		Map<String, Object> parentDoc = null;
		GetResponse response = client.prepareGet(INDEX_PENDING_ALIAS, PENDING_TYPE, parentUid).get();
		if (response.isSourceEmpty()) {
			try {
				logger.warn("Pending index misses parent {} for imapUid {} in mailbox {}", parentUid,
						item.value.imapUid, mailboxUniqueId);
				parentDoc = reloadFromDb(parentUid, mailboxUniqueId, mail);
			} catch (Exception e) {
				logger.warn("Cannot resync pending data", e);
			}
		} else {
			parentDoc = response.getSource();
		}

		if (parentDoc == null || parentDoc.isEmpty()) {
			logger.info("Skipping indexation of {}:{}", mailboxUniqueId, parentUid);
			return;
		}

		Map<String, Object> mutableContent = new HashMap<>(parentDoc);

		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) parentDoc.get("headers");
		if (headers.containsKey("x-bm-event")) {
			is.add("meeting");
		}
		if (headers.containsKey("x-asterisk-callerid")) {
			is.add("voicemail");
		}

		mutableContent.put("owner", user);
		mutableContent.put("in", mailboxUniqueId);
		mutableContent.put("uid", mail.imapUid);
		mutableContent.put("id", id);
		mutableContent.put("is", is);
		mutableContent.put("itemId", item.internalId);
		mutableContent.put("parentId", parentUid);
		if (mail.internalDate != null) {
			mutableContent.put("internalDate", mail.internalDate.toInstant().toString());
		}
		mutableContent.put(JOIN_FIELD, ImmutableMap.of("name", CHILD_TYPE, "parent", parentUid));

		// deduplicate fields
		mutableContent.remove("content");
		mutableContent.remove("messageId");
		mutableContent.remove("references");

		String route = "partition_xxx";
		GetResponse hasParent = client.prepareGet(userAlias, MAILSPOOL_TYPE, parentUid).setFetchSource(false).get();
		if (!hasParent.isExists()) {
			parentDoc.remove("with");
			parentDoc.remove("headers");
			parentDoc.remove("size");
			parentDoc.remove("filename");
			parentDoc.remove("has");
			parentDoc.remove("is");

			parentDoc.put(JOIN_FIELD, PARENT_TYPE);
			IndexRequestBuilder parentIdxReq = client.prepareIndex(userAlias, MAILSPOOL_TYPE).setSource(parentDoc)//
					.setId(parentUid).setRouting(route);
			if (bulk.isPresent()) {
				bulk.map(EsBulk.class::cast).ifPresent(bulkImpl -> bulkImpl.bulk.add(parentIdxReq));
			} else {
				parentIdxReq.execute().actionGet();
			}
		}

		IndexRequestBuilder childIdxReq = client.prepareIndex(userAlias, MAILSPOOL_TYPE).setSource(mutableContent)//
				.setId(id).setRouting(route);
		if (bulk.isPresent()) {
			bulk.map(EsBulk.class::cast).ifPresent(bulkImpl -> bulkImpl.bulk.add(childIdxReq));
		} else {
			childIdxReq.execute().actionGet();
		}
	}

	private Map<String, Object> reloadFromDb(String uid, String mailboxUniqueId, MailboxRecord mail)
			throws InterruptedException, ExecutionException, TimeoutException {
		IDbMailboxRecords service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, mailboxUniqueId);
		Stream eml = service.fetchComplete(mail.imapUid);
		IndexedMessageBody indexData = IndexedMessageBody.createIndexBody(uid, eml);
		return storeBody(indexData);
	}

	@Override
	public void deleteBox(ItemValue<Mailbox> box, String folderUid) {

		logger.debug("deleteBox {} {}", box.uid, folderUid);
		boolean exist = ensureAliasExists(getIndexAliasName(box.uid), getIndexClient());
		if (!exist) {
			return;
		}
		QueryBuilder q = QueryBuilders.constantScoreQuery(asFilter(folderUid));

		long count = bulkDelete(getIndexAliasName(box.uid), q);
		logger.info("deleteBox {}:{} :  {} deleted", box.uid, folderUid, count);

		cleanupParents(getIndexAliasName(box.uid));
	}

	@Override
	public void expunge(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, IDSet set) {
		logger.info("(expunge) expunge: {} {}", f.displayName, set);

		long deletedCount = deleteSet(box, f, set);

		logger.info("expunge {} ({}) : {} deleted", f.displayName, set, deletedCount);
	}

	private void cleanupFolder(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, IDSet idSet) {
		logger.info("(cleanupFolder) expunge: {} {}", f.displayName, idSet);

		long deletedCount = deleteSet(box, f, idSet);

		if (deletedCount > 0) {
			logger.warn("cleanup of {} {} was needed : {}", f, idSet, deletedCount);
		}
	}

	private long deleteSet(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, IDSet set) {
		long deletedCount = 0;
		Iterator<IDRange> iter = set.iterator();
		while (iter.hasNext()) {
			BoolQueryBuilder filter = QueryBuilders.boolQuery().must(asFilter(f.uid)).must(asFilter(iter, 1000));
			QueryBuilder q = QueryBuilders.constantScoreQuery(filter);
			deletedCount += bulkDelete(getIndexAliasName(box.uid), q);
		}

		cleanupParents(getIndexAliasName(box.uid));

		return deletedCount;
	}

	@Override
	public void cleanupFolder(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, Set<Integer> set) {
		List<Integer> docIds = new ArrayList<>(set);
		for (Integer uid : set) {
			docIds.add(uid);
		}
		Collections.sort(docIds);
		IDSet idSet = IDSet.create(docIds);
		cleanupFolder(box, f, idSet);
	}

	public static Client getIndexClient() {
		return ESearchActivator.getClient();
	}

	private long bulkDelete(String indexName, QueryBuilder q) {
		return new DeleteByQueryRequestBuilder(getIndexClient(), DeleteByQueryAction.INSTANCE).filter(q)
				.source(indexName).get().getDeleted();
	}

	private QueryBuilder asFilter(Iterator<IDRange> iter, int max) {
		BoolQueryBuilder orBuilder = QueryBuilders.boolQuery();
		int count = 0;
		while (iter.hasNext() && count++ < max) {
			IDRange range = iter.next();
			orBuilder(orBuilder, range);
		}
		return orBuilder;
	}

	private QueryBuilder asFilter(String uid) {
		return QueryBuilders.termQuery("in", uid);
	}

	private QueryBuilder asFilter(IDSet set) {
		BoolQueryBuilder orBuilder = QueryBuilders.boolQuery();
		for (IDRange range : set) {
			orBuilder(orBuilder, range);
		}
		return orBuilder;
	}

	private void orBuilder(BoolQueryBuilder orBuilder, IDRange range) {
		logger.debug("range {}", range);
		if (range.isUnique()) {
			orBuilder.should(QueryBuilders.termQuery("uid", range.from()));
		} else if (range.to() < 0) {
			orBuilder.should(QueryBuilders.rangeQuery("uid").from(range.from()));
		} else {
			// range with limit
			orBuilder.should(QueryBuilders.rangeQuery("uid").from(range.from()).to(range.to()));
		}
	}

	@Override
	public List<MailSummary> fetchSummary(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, IDSet set) {
		QueryBuilder query = QueryBuilders.boolQuery().must(asFilter(f.uid)).filter(asFilter(set));
		query = QueryBuilders.constantScoreQuery(query);
		return fetchSummary(query, box.uid);
	}

	private void cleanupParents(final String alias) {
		/*
		 * To be able to retrieve entries without an explicit owner field pointing to
		 * the user this alias belongs to, we need to resolve the physical index name
		 * this alias is assigned to.
		 */
		String index = getUserAliasIndex(alias, getIndexClient());
		logger.info("Cleaning up parent-child hierarchy of alias/index {}/{}", alias, index);
		VertxPlatform.eventBus().publish("index.mailspool.cleanup", new JsonObject().put("index", index));
	}

	private boolean ensureAliasExists(String alias, Client client) {
		try {
			GetAliasesResponse t = client.admin().indices().prepareGetAliases(alias).execute().actionGet();
			return !t.getAliases().isEmpty();
		} catch (Exception e) {
			logger.error("ensureAliasExists({})", alias, e);
			return false;
		}
	}

	private String getUserAliasIndex(String alias, Client client) {
		try {
			GetAliasesResponse t = client.admin().indices().prepareGetAliases(alias).execute().actionGet();
			return t.getAliases().keysIt().next();
		} catch (Exception e) {
			logger.error("getUserAliasIndex({})", alias, e);
			return alias;
		}
	}

	@SuppressWarnings("unchecked")
	private List<MailSummary> fetchSummary(QueryBuilder query, String entityId) {
		final Client client = getIndexClient();

		QueryBuilder withNeededFields = Queries.and(//
				QueryBuilders.existsQuery("uid"), //
				QueryBuilders.existsQuery("is"), //
				QueryBuilders.existsQuery("parentId"), //
				query);
		String[] sourceIncludeFields = { "uid", "is", "parentId" };
		List<MailSummary> ret = new ArrayList<>();
		String index = getIndexAliasName(entityId);

		try (Pit pit = Pit.allocate(client, index, 60)) {
			do {
				SearchRequestBuilder searchBuilder = client.prepareSearch(index) //
						.setQuery(withNeededFields) //
						.setFetchSource(sourceIncludeFields, null) //
						.setPointInTime(new PointInTimeBuilder(pit.id)) //
						.setTypes(MAILSPOOL_TYPE) //
						.addSort(SortBuilders.fieldSort("_shard_doc").order(SortOrder.ASC)) //
						.setTrackTotalHits(false) //
						.setSize(SIZE);
				pit.adaptSearch(searchBuilder);
				SearchResponse r = searchBuilder.execute().actionGet();

				if (r.getHits() != null && r.getHits().getHits() != null) {
					for (SearchHit h : r.getHits().getHits()) {
						Map<String, Object> source = h.getSourceAsMap();
						MailSummary sum = new MailSummary();
						sum.uid = source.get("uid") != null ? (int) source.get("uid") : null;
						sum.flags = new HashSet<>((List<String>) source.get("is"));
						sum.parentId = (String) source.get("parentId");
						pit.consumeHit(h);
						ret.add(sum);
					}
				}
			} while (pit.hasNext());
		} catch (Exception e) {
			throw new ServerFault(e);
		}

		return ret;
	}

	@Override
	public void syncFlags(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, List<MailSummary> mails) {
		if (mails.isEmpty())
			return;
		Client client = getIndexClient();

		BulkRequestBuilder bulk = client.prepareBulk();
		for (MailSummary sum : mails) {
			String id = f.uid + ":" + sum.uid;
			UpdateRequestBuilder urb = client.prepareUpdate().setIndex(getIndexAliasName(box.uid))
					.setType(MAILSPOOL_TYPE).setId(id);
			urb.setRouting(sum.parentId);
			if (logger.isDebugEnabled()) {
				logger.debug("update {} flags {} parentId {}", id, sum.flags, sum.parentId);
			}
			urb.setDoc("is", sum.flags);
			bulk.add(urb);
		}

		bulk.execute().actionGet().getItems();

	}

	@Override
	public double getArchivedMailSum(String userEntityId) {
		final Client client = getIndexClient();

		QueryBuilder q = QueryBuilders.boolQuery().must(QueryBuilders.termsQuery("owner", userEntityId))
				.must(QueryBuilders.termQuery("is", "bmarchived"));

		SumAggregationBuilder a = AggregationBuilders.sum("archivemailsizesum").field("size");

		SearchResponse r = client.prepareSearch(getIndexAliasName(userEntityId)).setQuery(q).addAggregation(a)
				.setFetchSource(false).execute().actionGet();

		InternalSum sum = (InternalSum) r.getAggregations().get("archivemailsizesum");
		return sum.getValue();
	}

	@Override
	public void createMailbox(String mailboxUid) {
		repairMailbox(mailboxUid, new NullTaskMonitor());
	}

	@Override
	public Set<String> getFolders(String entityId) {
		final Client client = getIndexClient();

		final String indexName = getIndexAliasName(entityId);

		QueryBuilder q = QueryBuilders.termQuery("owner", entityId);
		SearchResponse r = client.prepareSearch(indexName).setTypes(MAILSPOOL_TYPE) //
				.setQuery(q).addAggregation(AggregationBuilders.terms("in").field("in")).execute().actionGet();

		StringTerms values = r.getAggregations().get("in");

		return values.getBuckets().stream().map(a -> (String) a.getKey()).collect(Collectors.toSet());
	}

	@Override
	public void deleteMailbox(String entityId) {
		final Client client = getIndexClient();

		QueryBuilder q = QueryBuilders.termQuery("owner", entityId);

		long deletedCount = bulkDelete(getIndexAliasName(entityId), q);
		logger.debug("deleteBox {} : {} deleted", entityId, deletedCount);

		try {
			client.admin().indices().prepareAliases().removeAlias("mailspool", getIndexAliasName(entityId)).execute()
					.actionGet();
		} catch (ElasticsearchException e) {
			logger.warn("Problem removing index or alias for mailbox {} {}", entityId, e.getMessage());
		}
	}

	@Override
	public void repairMailbox(String entityId, IServerTaskMonitor monitor) {
		monitor.begin(3, "Check index state for mailbox");
		final Client client = getIndexClient();
		if (client == null) {
			logger.warn("elasticsearch in not (yet) available");
			return;
		}

		GetIndexResponse resp = client.admin().indices().prepareGetIndex().addIndices("mailspool*").get();
		List<String> shards = filterMailspoolIndexNames(resp);

		if (shards.isEmpty()) {
			logger.warn("no shards found");
			return;
		}

		GetAliasesResponse t = client.admin().indices().prepareGetAliases(getIndexAliasName(entityId)).execute()
				.actionGet();

		if (t != null && t.getAliases().isEmpty() && client.admin().indices().prepareExists(getIndexAliasName(entityId))
				.execute().actionGet().isExists()) {
			// an index has been created, we need an alias here
			logger.info("indice {} is not an alias, delete it ", getIndexAliasName(entityId));
			client.admin().indices().prepareDelete(getIndexAliasName(entityId)).execute().actionGet();
			monitor.log(String.format("indice %s is not an alias, delete it ", getIndexAliasName(entityId)));
		}

		if (t == null || t.getAliases().isEmpty()) {
			monitor.progress(1, "no alias, check mailspool index");
			monitor.progress(1, String.format("create alias %s from mailspool ", getIndexAliasName(entityId)));
			String indexName = MailIndexActivator.getMailIndexHook().getMailspoolIndexName(client, shards, entityId);

			logger.info("create alias {} from {} ", getIndexAliasName(entityId), indexName);
			client.admin().indices().prepareAliases()
					.addAlias(indexName, getIndexAliasName(entityId), QueryBuilders.termQuery("owner", entityId))
					.execute().actionGet();

		}
	}

	@Override
	public boolean checkMailbox(String entityId) {
		final Client client = getIndexClient();
		if (client == null) {
			logger.warn("elasticsearch in not (yet) available");
			return true;
		}

		GetAliasesResponse t = client.admin().indices().prepareGetAliases(getIndexAliasName(entityId)).execute()
				.actionGet();

		return (t != null && !t.getAliases().isEmpty());
	}

	@Override
	public void moveMailbox(String mailboxUid, String indexName) {

		Client client = ESearchActivator.getClient();

		IndicesExistsResponse resp = client.admin().indices().prepareExists(indexName).get();
		if (!resp.isExists()) {
			// create new index if doesnt exsist
			client.admin().indices().prepareCreate(indexName)
					.setSource(ESearchActivator.getIndexSchema("mailspool"), XContentType.JSON).execute().actionGet();
			ClusterHealthResponse healthResp = client.admin().cluster().prepareHealth(indexName).setWaitForGreenStatus()
					.execute().actionGet();
			logger.debug("index health response: {}", healthResp);

		}

		// retrieve "from" indexName
		GetAliasesResponse aliasResp = client.admin().indices().prepareGetAliases(getIndexAliasName(mailboxUid)).get();
		String fromIndex = aliasResp.getAliases().keysIt().next();

		// move alias
		client.admin().indices().prepareAliases().removeAlias(fromIndex, getIndexAliasName(mailboxUid))
				.addAlias(indexName, getIndexAliasName(mailboxUid), QueryBuilders.termQuery("owner", mailboxUid)).get();

		// bulk copy mails
		// msg body
		ReindexRequestBuilder builder = new ReindexRequestBuilder(client, ReindexAction.INSTANCE).source(fromIndex)
				.destination(indexName);
		builder.destination().setOpType(OpType.INDEX);
		builder.abortOnVersionConflict(false);
		builder.filter(JoinQueryBuilders.hasChildQuery(CHILD_TYPE, QueryBuilders.termQuery("owner", mailboxUid),
				ScoreMode.None));
		BulkByScrollResponse copyResp = builder.get();
		if (!copyResp.getBulkFailures().isEmpty()) {
			logger.error("copy failure : {}", copyResp.getBulkFailures());
		}
		logger.info("bulk copy of msgBody response {}", copyResp);

		// copy msg
		builder = new ReindexRequestBuilder(client, ReindexAction.INSTANCE).source(fromIndex).destination(indexName);
		builder.destination().setOpType(OpType.INDEX);
		builder.abortOnVersionConflict(false);
		builder.filter(QueryBuilders.termQuery("owner", mailboxUid));
		builder.refresh(true);
		copyResp = builder.get();
		if (!copyResp.getBulkFailures().isEmpty()) {
			logger.error("copy failure : {}", copyResp.getBulkFailures());
		}

		logger.info("bulk copy of msg response {}", copyResp);

		bulkDelete(fromIndex, QueryBuilders.termQuery("owner", mailboxUid));

		VertxPlatform.eventBus().publish("index.mailspool.cleanup", new JsonObject().put("index", fromIndex));
	}

	public List<ShardStats> getStats() {
		Client client = ESearchActivator.getClient();
		GetIndexResponse resp = client.admin().indices().prepareGetIndex().addIndices("mailspool*").get();

		List<ShardStats> ret = new ArrayList<>(resp.indices().length);
		logger.debug("indices {} ", (Object) resp.indices());

		long worstResponseTime = 0;

		for (String indexName : filterMailspoolIndexNames(resp)) {
			ShardStats is = new ShardStats();

			IndexStats stat = client.admin().indices().prepareStats(indexName).get().getIndex(indexName);
			is.size = stat.getTotal().store.getSizeInBytes();
			SearchResponse aggResp = client.prepareSearch(indexName)
					.addAggregation(AggregationBuilders.terms("countByOwner").size(100).field("owner")).get();

			StringTerms agg = aggResp.getAggregations().get("countByOwner");

			is.topMailbox = agg.getBuckets().stream().map(b -> {
				MailboxStats as = new ShardStats.MailboxStats();
				as.mailboxUid = b.getKeyAsString();
				as.docCount = b.getDocCount();
				return as;
			}).collect(Collectors.toList());

			GetAliasesResponse aliasesRsp = client.admin().indices().prepareGetAliases().addIndices(indexName).get();

			List<AliasMetadata> indexAliases = aliasesRsp.getAliases().get(indexName);
			if (indexAliases == null) {
				is.mailboxes = Collections.emptySet();
			} else {
				is.mailboxes = indexAliases.stream() //
						.filter(a -> a.getAlias().startsWith("mailspool_alias_"))
						.map(am -> am.getAlias().substring("mailspool_alias_".length()))//
						.collect(Collectors.toSet());
			}

			is.docCount = stat.getTotal().docs.getCount();
			is.indexName = indexName;

			is.state = ShardStats.State.OK;

			// random search on top mailbox
			// 0 to 500ms -> OK
			// 500ms to 1000ms -> HALF_FULL
			// > 1000ms -> FULL

			if (!is.topMailbox.isEmpty()) {
				MailboxStats topMailbox = is.topMailbox.get(0);
				String randomToken = Long.toHexString(Double.doubleToLongBits(Math.random()));
				QueryBuilder q = QueryBuilders.boolQuery()//
						.must(JoinQueryBuilders.hasParentQuery(PARENT_TYPE,
								QueryBuilders.queryStringQuery("content:\"" + randomToken + "\""), false));
				SearchResponse results = client.prepareSearch(getIndexAliasName(topMailbox.mailboxUid))//
						.setQuery(q).setFetchSource(true).setTypes(MAILSPOOL_TYPE).execute().actionGet();

				long duration = results.getTook().millis();
				if (duration > 1000) {
					is.state = ShardStats.State.FULL;
				} else if (duration > 500) {
					is.state = ShardStats.State.HALF_FULL;
				}

				worstResponseTime = Math.max(worstResponseTime, duration);

				logger.info("{} response time : {}ms, state : {}", is.indexName, duration, is.state);
				metricRegistry.timer(idFactory.name("response-time", "index", is.indexName)).record(duration,
						TimeUnit.MILLISECONDS);

			}

			ret.add(is);
		}

		metricRegistry.gauge(idFactory.name("worst-response-time")).set(worstResponseTime);

		Collections.sort(ret, (a, b) -> (int) (b.docCount - a.docCount));
		return ret;
	}

	@Override
	public List<SimpleShardStats> getLiteStats() {
		Client client = ESearchActivator.getClient();
		GetIndexResponse resp = client.admin().indices().prepareGetIndex().addIndices("mailspool*").get();

		List<SimpleShardStats> ret = new ArrayList<>(resp.indices().length);
		logger.debug("indices {} ", (Object) resp.indices());

		for (String indexName : filterMailspoolIndexNames(resp)) {
			SimpleShardStats is = new SimpleShardStats();

			IndexStats stat = client.admin().indices().prepareStats(indexName).get().getIndex(indexName);
			is.size = stat.getTotal().store.getSizeInBytes();

			GetAliasesResponse aliasesRsp = client.admin().indices().prepareGetAliases().addIndices(indexName).get();

			List<AliasMetadata> indexAliases = aliasesRsp.getAliases().get(indexName);
			if (indexAliases == null) {
				is.mailboxes = Collections.emptySet();
			} else {
				is.mailboxes = indexAliases.stream() //
						.filter(a -> a.getAlias().startsWith("mailspool_alias_"))
						.map(am -> am.getAlias().substring("mailspool_alias_".length()))//
						.collect(Collectors.toSet());
			}

			is.docCount = stat.getTotal().docs.getCount();
			is.indexName = indexName;

			ret.add(is);
		}

		Collections.sort(ret, (a, b) -> (int) (b.docCount - a.docCount));
		return ret;
	}

	private static final long TIME_BUDGET = TimeUnit.SECONDS.toNanos(15);

	@Override
	public SearchResult searchItems(String dirEntryUid, MailboxFolderSearchQuery searchQuery) {
		SearchQuery query = searchQuery.query;
		Client client = ESearchActivator.getClient();
		String index = getIndexAliasName(dirEntryUid);
		SearchRequestBuilder searchBuilder = client.prepareSearch(index);
		QueryBuilder bq = buildEsQuery(query);

		searchBuilder.setQuery(bq);
		searchBuilder.setFetchSource(true);
		searchBuilder.setTrackTotalHits(true);

		if (searchQuery.sort != null && searchQuery.sort.hasCriterias()) {
			searchQuery.sort.criteria
					.forEach(c -> searchBuilder.addSort(c.field, SortOrder.fromString(c.order.name())));
		} else {
			searchBuilder.addSort("date", SortOrder.DESC);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("{}", searchBuilder);
		}

		try {
			if (query.offset == 0 && query.maxResults >= Integer.MAX_VALUE) {
				return paginatedSearch(dirEntryUid, client, searchBuilder, index);
			} else {
				return simpleSearch(dirEntryUid, searchBuilder, query);
			}
		} catch (Exception e) {
			logger.warn("Failed to search {} ({})", searchBuilder, e.getMessage());
			return SearchResult.noResult();
		}
	}

	private SearchResult paginatedSearch(String dirEntryUid, Client client, SearchRequestBuilder searchBuilder,
			String index) throws Exception {
		searchBuilder.setSize(1000);

		int deduplicated = 0;
		Map<Integer, InternalMessageSearchResult> results = new LinkedHashMap<>();
		long totalHits = 0;
		int handled = 0;

		try (Pit pit = Pit.allocateUsingTimebudget(client, index, 60, TIME_BUDGET)) {
			do {
				searchBuilder.setPointInTime(new PointInTimeBuilder(pit.id));
				pit.adaptSearch(searchBuilder);
				SearchResponse sr = searchBuilder.execute().actionGet();
				SearchHits searchHits = sr.getHits();
				if (totalHits == 0) {
					totalHits = searchHits.getTotalHits().value;
					searchBuilder.setTrackTotalHits(false);
				}
				if (sr.getHits() != null && sr.getHits().getHits() != null) {
					for (SearchHit h : sr.getHits().getHits()) {
						handled++;
						pit.consumeHit(h);
						deduplicated += handleAndGetDeduplicatedHits(results, h);
					}
				}
			} while (pit.hasNext());
		}

		return createResult(dirEntryUid, totalHits, handled, results, deduplicated);
	}

	private SearchResult simpleSearch(String dirEntryUid, SearchRequestBuilder searchBuilder, SearchQuery query) {
		searchBuilder.setFrom((int) query.offset);
		searchBuilder.setSize((int) query.maxResults);

		SearchResponse sr = searchBuilder.execute().actionGet();
		SearchHits searchHits = sr.getHits();

		Map<Integer, InternalMessageSearchResult> results = new LinkedHashMap<>();
		int deduplicated = 0;
		for (SearchHit sh : searchHits.getHits()) {
			deduplicated += handleAndGetDeduplicatedHits(results, sh);
		}
		return createResult(dirEntryUid, searchHits.getTotalHits().value, searchHits.getHits().length, results,
				deduplicated);
	}

	private SearchResult createResult(String dirEntryUid, long totalHits, int handled,
			Map<Integer, InternalMessageSearchResult> results, int deduplicated) {
		SearchResult result = new SearchResult();
		result.results = new ArrayList<>(results.values());
		result.totalResults = (int) (totalHits - deduplicated);
		result.hasMoreResults = totalHits > results.size();
		logger.info("[{}] results: {} (tried {}) / {}, hasMore: {}", dirEntryUid, results.size(), handled,
				result.totalResults, result.hasMoreResults);
		return result;
	}

	private int handleAndGetDeduplicatedHits(Map<Integer, InternalMessageSearchResult> results, SearchHit sh) {
		return safeResult(sh).map(result -> {
			if (results.containsKey(result.itemId)) {
				if (results.get(result.itemId).imapUid < result.imapUid) {
					results.put(result.itemId, result);
				}
				return 1;
			} else {
				results.put(result.itemId, result);
				return 0;
			}
		}).orElse(0);
	}

	private QueryBuilder buildEsQuery(SearchQuery query) {
		BoolQueryBuilder bq = QueryBuilders.boolQuery();

		if (query.scope.folderScope != null && query.scope.folderScope.folderUid != null) {
			bq.must(QueryBuilders.termQuery("in", query.scope.folderScope.folderUid));
		}

		bq.mustNot(QueryBuilders.termQuery("is", "deleted"));
		bq = addSearchQuery(bq, query.query);
		bq = addSearchRecordQuery(bq, query.recordQuery);
		bq = addPreciseSearchQuery(bq, "messageId", query.messageId);
		bq = addPreciseSearchQuery(bq, "references", query.references);

		if (query.headerQuery != null && !query.headerQuery.query.isEmpty()) {
			List<QueryBuilder> builders = new ArrayList<>(query.headerQuery.query.size());
			for (SearchQuery.Header headerQuery : query.headerQuery.query) {
				String queryString = "headers." + headerQuery.name.toLowerCase() + ":\"" + headerQuery.value + "\"";
				builders.add(QueryBuilders.queryStringQuery(queryString));
			}
			if (query.headerQuery.logicalOperator == LogicalOperator.AND) {
				bq = bq.must(Queries.and(builders));
			} else {
				bq = bq.must(Queries.or(builders));
			}
		}
		return bq;
	}

	private Optional<InternalMessageSearchResult> safeResult(SearchHit sh) {
		try {
			return Optional.of(createSearchResult(sh));
		} catch (Exception e) {
			logger.warn("Cannot create result object", e);
			return Optional.empty();
		}
	}

	private BoolQueryBuilder addSearchQuery(BoolQueryBuilder bq, String query) {
		if (!Strings.isNullOrEmpty(query)) {
			return bq.must(JoinQueryBuilders.hasParentQuery(PARENT_TYPE,
					QueryBuilders.queryStringQuery(query).defaultField("content").defaultOperator(Operator.AND),
					false));
		} else {
			return bq;
		}
	}

	private BoolQueryBuilder addSearchRecordQuery(BoolQueryBuilder bq, String query) {
		if (!Strings.isNullOrEmpty(query)) {
			return bq.must(QueryBuilders.queryStringQuery(query));
		} else {
			return bq;
		}
	}

	private BoolQueryBuilder addPreciseSearchQuery(BoolQueryBuilder bq, String searchField, String searchValue) {
		if (searchValue != null) {
			return bq.must(JoinQueryBuilders.hasParentQuery(PARENT_TYPE,
					QueryBuilders.termQuery(searchField, searchValue), false));
		} else {
			return bq;
		}
	}

	@SuppressWarnings({ "unchecked" })
	private InternalMessageSearchResult createSearchResult(SearchHit sh) {
		Map<String, Object> source = sh.getSourceAsMap();
		Integer itemId = source.get("itemId") != null ? (int) source.get("itemId") : null;
		String folderUid = ((String) source.get("id")).split(":")[0];
		String contUid = "mbox_records_" + folderUid;
		String subject = (String) source.get("subject");
		logger.debug("matching result itemId:{} subject:'{}' in folder:{}", itemId, subject, folderUid);
		int size = (int) source.get("size");

		String internalDate = (String) source.get("internalDate");
		ZonedDateTime date;
		if (internalDate != null) {
			date = ZonedDateTime.parse(internalDate);
		} else {
			date = ZonedDateTime.parse((String) source.get("date"));
		}
		Date messageDate = Date.from(date.toInstant());

		List<String> flags = (List<String>) source.get("is");
		boolean seen = flags.contains("seen");
		boolean flagged = flags.contains("flagged");

		Map<String, String> headers = (Map<String, String>) source.get("headers");

		Mbox to = Mbox.create("unknown", "unknown");
		try {
			InternetAddress[] addrList = InternetAddress.parse(Optional.ofNullable(headers.get("to")).orElse(""));
			if (addrList.length > 0) {
				InternetAddress mboxTo = addrList[0];
				to = Mbox.create(mboxTo.getPersonal(), mboxTo.getAddress());
			}
		} catch (AddressException e) {
			logger.warn("Failed to parse TO {}", headers.get("to"));
		}

		Mbox from = Mbox.create("unknown", "unknown");
		try {
			EmailAddress mboxFrom = new EmailAddress(headers.get("from"));
			from = Mbox.create(mboxFrom.getPersonal(), mboxFrom.getAddress(), "SMTP");
		} catch (AddressException e) {
			logger.warn("Failed to parse FROM {}", headers.get("from"));
		}
		boolean hasAttachment = !((List<String>) source.get("has")).isEmpty();

		String preview = Strings.nullToEmpty((String) source.get("preview"));

		int imapUid = source.get("uid") != null ? (Integer) source.get("uid") : 0;

		return new InternalMessageSearchResult(contUid, itemId, subject, size, "IPM.Note", messageDate, from, to, seen,
				flagged, hasAttachment, preview, imapUid);
	}

	public static class InternalMessageSearchResult extends MessageSearchResult {
		public final int imapUid;

		public InternalMessageSearchResult(String contUid, int itemId, String subject, int size, String string,
				Date messageDate, Mbox from, Mbox to, boolean seen, boolean flagged, boolean hasAttachment,
				String preview, int imapUid) {
			super(contUid, itemId, subject, size, string, messageDate, from, to, seen, flagged, hasAttachment, preview);
			this.imapUid = imapUid;
		}

	}

}