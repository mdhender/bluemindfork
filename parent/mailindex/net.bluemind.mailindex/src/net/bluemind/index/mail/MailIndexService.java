package net.bluemind.index.mail;

import static java.util.Collections.singletonList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Streams;
import com.netflix.spectator.api.Registry;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch._types.OpType;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.ConstantScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.ReindexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import co.elastic.clients.elasticsearch.indices.stats.IndicesStats;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MessageSearchResult;
import net.bluemind.backend.mail.api.MessageSearchResult.Mbox;
import net.bluemind.backend.mail.api.SearchQuery;
import net.bluemind.backend.mail.api.SearchQuery.LogicalOperator;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.api.SearchSort;
import net.bluemind.backend.mail.api.utils.MailIndexQuery;
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
import net.bluemind.lib.elasticsearch.EsBulk;
import net.bluemind.lib.elasticsearch.IndexAliasMapping;
import net.bluemind.lib.elasticsearch.IndexAliasMode;
import net.bluemind.lib.elasticsearch.IndexAliasMode.Mode;
import net.bluemind.lib.elasticsearch.MailspoolStats;
import net.bluemind.lib.elasticsearch.MailspoolStats.FolderCount;
import net.bluemind.lib.elasticsearch.Pit;
import net.bluemind.lib.elasticsearch.Pit.PaginableSearchQueryBuilder;
import net.bluemind.lib.elasticsearch.Pit.PaginationParams;
import net.bluemind.lib.elasticsearch.Queries;
import net.bluemind.lib.elasticsearch.VertxEsTaskMonitor;
import net.bluemind.lib.elasticsearch.exception.ElasticDocumentException;
import net.bluemind.lib.elasticsearch.exception.ElasticIndexException;
import net.bluemind.lib.elasticsearch.exception.ElasticTaskException;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.ShardStats;
import net.bluemind.mailbox.api.ShardStats.MailboxStats;
import net.bluemind.mailbox.api.SimpleShardStats;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.utils.ByteSizeUnit;
import net.bluemind.utils.EmailAddress;

public class MailIndexService implements IMailIndexService {
	public static final int SIZE = 200;

	private static final List<String> DEFAULT_QUERY_STRING_FIELDS = Arrays //
			.asList("subject", "content", "filename", "from", "to", "cc");
	private static final Logger logger = LoggerFactory.getLogger(MailIndexService.class);
	public static final String JOIN_FIELD = "body_msg_link";
	public static final String PARENT_TYPE = "body";
	public static final String CHILD_TYPE = "record";
	private static final String INDEX_PENDING = "mailspool_pending";
	private static final String INDEX_PENDING_READ_ALIAS = "mailspool_pending_read_alias";
	private static final String INDEX_PENDING_WRITE_ALIAS = "mailspool_pending_write_alias";

	private Registry metricRegistry;
	private IdFactory idFactory;

	public String getReadIndexAliasName(String entityId) {
		return IndexAliasMapping.get().getReadAliasByMailboxUid(entityId);
	}

	public String getWriteIndexAliasName(String entityId) {
		return IndexAliasMapping.get().getWriteAliasByMailboxUid(entityId);
	}

	public MailIndexService() {
		metricRegistry = MetricsRegistry.get();
		idFactory = new IdFactory("mailindex-service", metricRegistry, MailIndexService.class);

		VertxPlatform.executeBlockingPeriodic(TimeUnit.HOURS.toMillis(1), i -> getStats());
	}

	public static ElasticsearchClient getIndexClient() {
		return ESearchActivator.getClient();
	}

	@Override
	public Map<String, Object> storeBody(IndexedMessageBody body) {
		logger.debug("Saving body {} to pending index", body);
		Map<String, Object> content = bodyToDocument(body);
		ElasticsearchClient esClient = getIndexClient();
		try {
			esClient.index(i -> i.index(INDEX_PENDING_WRITE_ALIAS).id(body.uid).document(content));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(INDEX_PENDING_WRITE_ALIAS, e);
		}
		return content;
	}

	@Override
	public void storeBodyAsByte(String uid, byte[] body) {
		logger.info("Restore {} to pending index", uid);
		ElasticsearchClient esClient = getIndexClient();
		try {
			esClient.index(i -> i.index(INDEX_PENDING_WRITE_ALIAS).id(uid).withJson(new ByteArrayInputStream(body)));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(INDEX_PENDING_WRITE_ALIAS, e);
		}
	}

	private Map<String, Object> bodyToDocument(IndexedMessageBody body) {
		Map<String, Object> content = new HashMap<>();
		content.put("content", body.content);
		content.put("messageId", body.messageId.toString());
		content.put("references", body.references.stream().map(Object::toString).toList());
		content.put("preview", body.preview);
		content.put("subject", body.subject.toString());
		content.put("subject_kw", body.subject.toString());
		content.put("headers", body.headers());
		content.putAll(body.data);
		return content;
	}

	@Override
	public void deleteBodyEntries(List<String> bodyIds) {
		ElasticsearchClient esClient = getIndexClient();
		deleteBodiesFromIndex(bodyIds, INDEX_PENDING_WRITE_ALIAS);
		filteredMailspoolIndexNames(esClient).forEach(index -> deleteBodiesFromIndex(bodyIds, index));
	}

	private void deleteBodiesFromIndex(List<String> deletedOrphanBodies, String index) {
		ElasticsearchClient esClient = getIndexClient();
		try {
			esClient.deleteByQuery(d -> d.index(index)
					.query(q -> q.constantScore(s -> s.filter(f -> f.ids(i -> i.values(deletedOrphanBodies))))));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(index, e);
		}
	}

	@Override
	public long resetMailboxIndex(String mailboxUid) {
		String index = getWriteIndexAliasName(mailboxUid);
		return bulkDelete(index, q -> q.term(t -> t.field("owner").value(mailboxUid)));
	}

	@Override
	public void deleteBox(ItemValue<Mailbox> box, String folderUid) {
		ElasticsearchClient esClient = getIndexClient();
		String boxAlias = getWriteIndexAliasName(box.uid);
		getUserAliasIndex(boxAlias, esClient).ifPresentOrElse(boxIndex -> {
			long count = bulkDelete(boxAlias, q -> q.bool(b -> b //
					.must(m -> m.term(t -> t.field("owner").value(box.uid)))
					.must(m -> m.term(t -> t.field("in").value(folderUid)))));
			logger.info("deleteBox {}:{} :  {} deleted", box.uid, folderUid, count);
			cleanupParents(boxAlias, boxIndex);
		}, () -> logger.error("Unable to delete mails in elasticsearch, alias not found (mailbox:{}, folder:{})",
				box.uid, folderUid));

	}

	private long deleteSet(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, IDSet set) {
		ElasticsearchClient esClient = getIndexClient();
		String boxAlias = getWriteIndexAliasName(box.uid);
		return getUserAliasIndex(boxAlias, esClient).map(boxIndex -> {
			long deletedCount = 0;
			Iterator<IDRange> iter = set.iterator();
			while (iter.hasNext()) {
				deletedCount += bulkDelete(boxAlias, q -> q.bool(b -> b //
						.must(m -> m.term(t -> t.field("owner").value(box.uid)))
						.must(m -> m.term(t -> t.field("in").value(f.uid))) //
						.must(asFilter(iter, 1000))));
			}
			cleanupParents(boxAlias, boxIndex);
			return deletedCount;
		}).orElseGet(() -> {
			logger.error("Unable to delete mails in elasticsearch, alias not found (mailbox:{}, folder:{}, set:{})",
					box.uid, f.uid, set);
			return 0l;
		});
	}

	private void cleanupParents(String boxAlias, String boxIndex) {
		/*
		 * To be able to retrieve entries without an explicit owner field pointing to
		 * the user this alias belongs to, we need to resolve the physical index name
		 * this alias is assigned to.
		 */
		logger.info("Cleaning up parent-child hierarchy of alias/index {}/{}", boxAlias, boxIndex);
		VertxPlatform.eventBus().publish("index.mailspool.cleanup", new JsonObject().put("index", boxIndex));
	}

	private Optional<String> getUserAliasIndex(String alias, ElasticsearchClient esClient) {
		try {
			GetAliasResponse response = esClient.indices().getAlias(a -> a.name(alias));
			return Optional.of(response.result().keySet().iterator().next());
		} catch (ElasticsearchException e) {
			logger.warn("Elasticsearch user alias is missing: '{}'", alias);
			return Optional.empty();
		} catch (IOException e) {
			logger.error("Unexcepted while looking for alias: '{}'", alias, e);
			return Optional.empty();
		}
	}

	private long bulkDelete(String indexName, Function<Query.Builder, ObjectBuilder<Query>> filter) {
		ElasticsearchClient esClient = getIndexClient();
		try {
			return esClient.deleteByQuery(d -> d.index(indexName) //
					.query(t -> t.constantScore(s -> s.filter(filter)))).deleted();
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(indexName, e);
		}
	}

	private Query asFilter(Iterator<IDRange> iter, int max) {
		BoolQuery.Builder builder = new BoolQuery.Builder();
		int count = 0;
		while (iter.hasNext() && count++ < max) {
			IDRange range = iter.next();
			orBuilder(builder, range);
		}
		return builder.build()._toQuery();
	}

	private void orBuilder(BoolQuery.Builder orBuilder, IDRange range) {
		logger.debug("range {}", range);
		if (range.isUnique()) {
			orBuilder.should(s -> s.term(t -> t.field("uid").value(range.from())));
		} else if (range.to() < 0) {
			orBuilder.should(s -> s.range(r -> r.field("uid").gte(JsonData.of(range.from()))));
		} else {
			orBuilder.should(s -> s //
					.range(r -> r.field("uid").gte(JsonData.of(range.from())).lte(JsonData.of(range.to()))));
		}
	}

	@Override
	public void doBulk(List<BulkOp> operations) {
		new EsBulk(getIndexClient()).commitAll(operations,
				(op, b) -> b.index(i -> i.index(op.index()).routing(op.routing()).id(op.id()).document(op.doc())));
	}

	@Override
	public Map<String, Object> fetchBody(String mailboxUniqueId, MailboxRecord value) {
		ElasticsearchClient esClient = getIndexClient();
		String uid = value.messageBody;
		return Optional.ofNullable(IndexableMessageBodyCache.bodies.getIfPresent(uid)).map(this::bodyToDocument)
				.orElseGet(() -> loadParentDoc(esClient, mailboxUniqueId, value));
	}

	@Override
	public List<BulkOp> storeMessage(String mailboxUniqueId, ItemValue<MailboxRecord> item, String user, boolean bulk) {
		ElasticsearchClient esClient = getIndexClient();
		List<BulkOp> bulkOperation = new ArrayList<>();
		MailboxRecord mail = item.value;
		String parentUid = mail.messageBody;
		String id = mailboxUniqueId + ":" + item.internalId;
		String userAlias = getWriteIndexAliasName(user);
		Set<String> is = MessageFlagsHelper.asFlags(mail.flags);

		Map<String, Object> parentDoc = Optional //
				.ofNullable(IndexableMessageBodyCache.bodies.getIfPresent(parentUid)).map(this::bodyToDocument) //
				.orElseGet(() -> loadParentDoc(esClient, mailboxUniqueId, item.value));

		if (parentDoc.isEmpty()) {
			logger.info("Skipping indexation of {}:{}", mailboxUniqueId, parentUid);
			return Collections.emptyList();
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
		mutableContent.put(JOIN_FIELD, Map.of("name", CHILD_TYPE, "parent", parentUid));

		// deduplicate fields
		mutableContent.remove("messageId");
		mutableContent.remove("references");
		// Those fields are used for search on the parent and not retrieved on the child
		mutableContent.remove("content");
		// headers recipients are retrieved and sortby, those are not used
		mutableContent.remove("from");
		mutableContent.remove("to");
		mutableContent.remove("cc");
		String route = "partition_xxx";
		boolean parentExists;
		try {
			parentExists = esClient.exists(e -> e.index(userAlias).id(parentUid)).value();
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(userAlias, e);
		}
		if (!parentExists) {
			parentDoc.remove("with");
			parentDoc.remove("filename");
			// this field are not used for search
			parentDoc.remove("headers");
			parentDoc.remove("size");
			// these fields are updated on the child
			parentDoc.remove("has");
			parentDoc.remove("is");
			// this field is used for sorting on the child
			parentDoc.remove("subject_kw");
			parentDoc.put(JOIN_FIELD, PARENT_TYPE);

			if (bulk) {
				bulkOperation.add(new BulkOp(userAlias, parentUid, route, parentDoc));
			} else {
				store(esClient, userAlias, parentUid, route, parentDoc);
			}
		}
		if (bulk) {
			bulkOperation.add(new BulkOp(userAlias, id, route, mutableContent));
		} else {
			store(esClient, userAlias, id, route, mutableContent);
		}

		return bulkOperation;
	}

	public void store(ElasticsearchClient esClient, String index, String id, String route,
			Map<String, Object> document) {
		try {
			esClient.index(i -> i.index(index).id(id).routing(route).document(document));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(index, e);
		}
	}

	private Map<String, Object> loadParentDoc(ElasticsearchClient esClient, String mailboxUniqueId,
			MailboxRecord value) {
		String parentUid = value.messageBody;
		GetResponse<ObjectNode> response = null;
		try {
			response = esClient.get(i -> i //
					.index(INDEX_PENDING_READ_ALIAS).id(parentUid), ObjectNode.class);
		} catch (ElasticsearchException | IOException e1) {
			logger.error("Failed to load parent id:{}, index:{}", parentUid, INDEX_PENDING_READ_ALIAS);
		}
		if (response == null || !response.found()) {
			try {
				logger.warn("Pending index misses parent {} for imapUid {} in mailbox {}", parentUid, value.imapUid,
						mailboxUniqueId);
				return reloadFromDb(parentUid, mailboxUniqueId, value);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return Collections.emptyMap();
			} catch (Exception e) {
				logger.warn("Cannot resync pending data", e);
				return Collections.emptyMap();
			}
		} else {
			ObjectNode node = response.source();
			return new ObjectMapper().convertValue(node, new TypeReference<Map<String, Object>>() {
			});
		}
	}

	private Map<String, Object> reloadFromDb(String parentUid, String mailboxUniqueId, MailboxRecord mail)
			throws InterruptedException, ExecutionException, TimeoutException {
		IDbMailboxRecords service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMailboxRecords.class, mailboxUniqueId);
		Stream eml = service.fetchComplete(mail.imapUid);
		IndexedMessageBody indexData = IndexedMessageBody.createIndexBody(parentUid, eml);
		return storeBody(indexData);
	}

	@Override
	public void expunge(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, IDSet set) {
		logger.info("(expunge) expunge: {} {}", f.displayName, set);
		long deletedCount = deleteSet(box, f, set);
		logger.info("expunge {} ({}) : {} deleted", f.displayName, set, deletedCount);
	}

	@Override
	public List<MailSummary> fetchSummary(ItemValue<Mailbox> box, ItemValue<MailboxFolder> folderItem, IDSet set) {
		ConstantScoreQuery.Builder builder = QueryBuilders.constantScore().filter(f -> f.bool(b -> b //
				.must(m -> m.hasParent(p -> p.parentType(PARENT_TYPE).query(q -> q.matchAll(a -> a)).score(false))) //
				.must(m -> m.term(t -> t.field("in").value(folderItem.uid))) //
				.must(m -> m.term(t -> t.field("owner").value(box.uid))) //
				.filter(asFilter(set)) //
		));
		return fetchSummary(builder.build()._toQuery(), box.uid);
	}

	@SuppressWarnings("unchecked")
	private List<MailSummary> fetchSummary(Query query, String entityId) {
		final ElasticsearchClient esClient = getIndexClient();

		String index = getReadIndexAliasName(entityId);
		QueryBuilders.bool().must( //
				QueryBuilders.exists(e -> e.field("uid")), //
				QueryBuilders.exists(e -> e.field("is")), //
				QueryBuilders.exists(e -> e.field("parentId")), //
				query);
		List<String> sourceIncludeFields = Arrays.asList("uid", "is", "parentId");
		List<MailSummary> summaries = new ArrayList<>();
		PaginableSearchQueryBuilder paginableSearch = s -> s //
				.source(so -> so.filter(f -> f.includes(sourceIncludeFields))) //
				.query(query);
		SortOptions sort = SortOptions.of(so -> so.field(f -> f.field("_shard_doc").order(SortOrder.Asc)));
		try (Pit<ObjectNode> pit = Pit.allocate(esClient, index, 60, ObjectNode.class)) {
			summaries = pit.allPages(paginableSearch, PaginationParams.all(sort, SIZE), this::toSummary);
		} catch (ElasticsearchException e) {
			SearchRequest request = paginableSearch.apply(new SearchRequest.Builder()).build();
			logger.error("Failed to fetch summary in {}, query (w/o Pit): {}", index, request);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

		return summaries;
	}

	private MailSummary toSummary(Hit<ObjectNode> hit) {
		ObjectNode source = hit.source();
		MailSummary sum = new MailSummary();
		sum.uid = source.get("uid") != null ? source.get("uid").asInt() : null;
		sum.flags = new HashSet<>();
		if (source.get("is").isArray()) {
			for (JsonNode flag : source.get("is")) {
				sum.flags.add(flag.asText());
			}
		}
		sum.parentId = source.get("parentId").asText();
		return sum;
	}

	private Query asFilter(IDSet set) {
		BoolQuery.Builder builder = new BoolQuery.Builder();
		for (IDRange range : set) {
			orBuilder(builder, range);
		}
		return builder.build()._toQuery();
	}

	@Override
	public void syncFlags(ItemValue<Mailbox> box, ItemValue<MailboxFolder> f, List<MailSummary> mails) {
		if (mails.isEmpty()) {
			return;
		}
		ElasticsearchClient esClient = getIndexClient();
		String boxAlias = getWriteIndexAliasName(box.uid);
		new EsBulk(esClient).commitAll(mails, (mail, b) -> b.update(u -> u //
				.index(boxAlias) //
				.routing(mail.parentId) //
				.id(f.uid + ":" + mail.uid) //
				.action(a -> a.doc(Map.of("is", mail.flags)))));
	}

	@Override
	public long getMailboxConsumedStorage(String userEntityId, ByteSizeUnit bsu) {
		final ElasticsearchClient esClient = getIndexClient();
		String index = getReadIndexAliasName(userEntityId);
		try {
			SearchResponse<Void> response = esClient.search(s -> s //
					.index(index) //
					.size(0).source(so -> so.fetch(false)) //
					.query(q -> q.bool(b -> b //
							.must(m -> m.term(t -> t.field("owner").value(userEntityId))) //
							.mustNot(m -> m.term(t -> t.field("is").value("deleted"))))) //
					.aggregations("quota", a -> a.sum(sum -> sum.field("size"))), //
					Void.class);
			double result = response.aggregations().get("quota").sum().value();
			return bsu.fromBytes((long) result);
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(index, e);
		}
	}

	@Override
	public Set<String> getFolders(String entityId) {
		final ElasticsearchClient esClient = getIndexClient();
		Query query = QueryBuilders.bool(b -> b.must(m -> m.term(t -> t.field("owner").value(entityId))));
		try {
			return new MailspoolStats(esClient).countAllFolders(entityId, 100, query) //
					.stream().map(FolderCount::folderUid).collect(Collectors.toSet());
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(getReadIndexAliasName(entityId), e);
		}
	}

	@Override
	public void createMailbox(String mailboxUid) {
		repairMailbox(mailboxUid, new NullTaskMonitor());
	}

	@Override
	public void deleteMailbox(String entityId) {
		final ElasticsearchClient esClient = getIndexClient();
		resetMailboxIndex(entityId);
		String boxAlias = getWriteIndexAliasName(entityId);
		try {
			esClient.indices().updateAliases(u -> u //
					.actions(a -> a.remove(r -> r.index("mailspool*").alias(boxAlias))));
		} catch (ElasticsearchException e) {
			logger.warn("[es][mailbox] Mailbox alias {} does not exists: {}", boxAlias, e.getMessage());
		} catch (IOException e) {
			logger.error("[es][mailbox] Unable to delete mailbox alias {}", boxAlias, e);
		}
	}

	@Override
	public void repairMailbox(String mailboxUid, IServerTaskMonitor monitor) {
		if (IndexAliasMode.getMode() == Mode.ONE_TO_ONE) {
			monitor.begin(3, "Check index state for mailbox");
			final ElasticsearchClient esClient = getIndexClient();
			if (esClient == null) {
				logger.warn("elasticsearch in not (yet) available");
				return;
			}

			List<String> shards = filteredMailspoolIndexNames(esClient);
			if (shards.isEmpty()) {
				logger.warn("no shards found");
				return;
			}

			String boxAlias = getWriteIndexAliasName(mailboxUid);
			boolean aliasExists = getUserAliasIndex(boxAlias, esClient).isPresent();
			try {
				if (!aliasExists && esClient.indices().exists(e -> e.index(boxAlias)).value()) {
					// an index has been created, we need an alias here
					logger.info("indice {} is not an alias, delete it ", boxAlias);
					esClient.indices().delete(d -> d.index(boxAlias));
					monitor.log(String.format("indice %s is not an alias, delete it ", boxAlias));
				}

				if (!aliasExists) {
					monitor.progress(1, "no alias, check mailspool index");
					monitor.progress(1, String.format("create alias %s from mailspool ", boxAlias));
					String indexName = MailIndexActivator.getMailIndexHook().getMailspoolIndexName(shards, mailboxUid);

					logger.info("create alias {} from {} ", boxAlias, indexName);
					esClient.indices().updateAliases(u -> u.actions(a -> a.add(ad -> ad //
							.index(indexName).alias(boxAlias)
							.filter(f -> f.term(t -> t.field("owner").value(mailboxUid))))));
				}
			} catch (ElasticsearchException | IOException e) {
				throw new ElasticIndexException(boxAlias, e);
			}
		}
	}

	@Override
	public boolean checkMailbox(String entityId) {
		final ElasticsearchClient esClient = getIndexClient();
		if (esClient == null) {
			logger.warn("elasticsearch in not (yet) available");
			return true;
		}

		return getUserAliasIndex(getReadIndexAliasName(entityId), esClient).isPresent();
	}

	@Override
	public void moveMailbox(String mailboxUid, String indexName, boolean deleteSource) {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		createMailspoolIfNotExists(indexName, esClient);

		// retrieve "from" indexName
		getUserAliasIndex(getWriteIndexAliasName(mailboxUid), esClient).ifPresentOrElse(fromIndex -> {
			// bulk copy mails
			moveMailspoolBox(esClient, mailboxUid, fromIndex, indexName);
			// move alias
			moveBoxAlias(esClient, mailboxUid, fromIndex, indexName);

			if (deleteSource) {
				bulkDelete(fromIndex, q -> q.term(t -> t.field("owner").value(mailboxUid)));
				VertxPlatform.eventBus().publish("index.mailspool.cleanup", new JsonObject().put("index", fromIndex));
			}
		}, () -> logger.error("Unable to move mailbox to {}, alias not found (mailbox:{})", indexName, mailboxUid));

	}

	private void createMailspoolIfNotExists(String indexName, ElasticsearchClient esClient) {
		boolean exists;
		try {
			exists = esClient.indices().exists(e -> e.index(indexName)).value();
			if (!exists) {
				// create new index if doesnt exsist
				esClient.indices().create(c -> c.index(indexName)
						.withJson(new ByteArrayInputStream(ESearchActivator.getIndexSchema("mailspool"))));
				HealthResponse health = esClient.cluster()
						.health(h -> h.index(indexName).waitForStatus(HealthStatus.Green));
				logger.debug("index health response: {}", health);
			}
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticIndexException(indexName, e);
		}
	}

	private void moveMailspoolBox(ElasticsearchClient esClient, String mailboxUid, String fromIndex, String toIndex) {
		try {
			VertxEsTaskMonitor taskMonitor = new VertxEsTaskMonitor(Vertx.vertx(), esClient);
			// msg body
			ReindexResponse parentResponse = esClient.reindex(r -> r //
					.waitForCompletion(false) //
					.source(s -> s //
							.index(fromIndex) //
							.size(1000) //
							.query(q -> q.hasChild(c -> c //
									.type(CHILD_TYPE) //
									.query(f -> f.term(t -> t.field("owner").value(mailboxUid))) //
									.scoreMode(ChildScoreMode.None)))) //
					.dest(d -> d.index(toIndex).opType(OpType.Index)) //
					.scroll(s -> s.time("1d")) //
					.conflicts(Conflicts.Proceed));
			jakarta.json.JsonObject parentStatus = taskMonitor.waitForCompletion(parentResponse.task()).toJson()
					.asJsonObject();
			List<String> parentStatusFailures = parentStatus.getJsonArray("failures").stream().map(Object::toString)
					.toList();
			if (!parentStatusFailures.isEmpty()) {
				logger.error("copy failure : {}", parentStatusFailures);
			}
			logger.info("bulk copy of msgBody response {}", parentStatus);

			// copy msg
			ReindexResponse childResponse = esClient.reindex(r -> r //
					.waitForCompletion(false) //
					.refresh(true) //
					.source(s -> s.index(fromIndex) //
							.size(1000) //
							.query(q -> q.term(t -> t.field("owner").value(mailboxUid)))) //
					.dest(d -> d.index(toIndex).opType(OpType.Index)) //
					.scroll(s -> s.time("1d")) //
					.conflicts(Conflicts.Proceed));
			jakarta.json.JsonObject childStatus = taskMonitor.waitForCompletion(childResponse.task()).toJson()
					.asJsonObject();
			List<String> childStatusFailures = parentStatus.getJsonArray("failures").stream().map(Object::toString)
					.toList();
			if (!childStatusFailures.isEmpty()) {
				logger.error("copy failure : {}", childStatusFailures);
			}
			logger.info("bulk copy of msg response {}", childStatus);
		} catch (ElasticsearchException | IOException | ElasticTaskException e) {
			throw new ElasticDocumentException(fromIndex, e);
		}
	}

	private void moveBoxAlias(ElasticsearchClient esClient, String mailboxUid, String fromIndex, String indexName) {
		String boxAlias = getWriteIndexAliasName(mailboxUid);
		try {
			esClient.indices().updateAliases(u -> u //
					.actions(a -> a.remove(r -> r.index(fromIndex).alias(boxAlias))) //
					.actions(a -> a.add(ad -> ad.index(indexName).alias(boxAlias)
							.filter(f -> f.term(t -> t.field("owner").value(mailboxUid))))));
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticIndexException(boxAlias, e);
		}
	}

	public List<ShardStats> getStats() {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		List<String> indexNames = filteredMailspoolIndexNames(esClient);
		List<ShardStats> ret = new ArrayList<>(indexNames.size());
		logger.debug("indices {} ", indexNames);

		long worstResponseTime = 0;
		for (String indexName : indexNames) {
			ShardStats is = indexStats(esClient, indexName, new ShardStats());

			is.topMailbox = topMailbox(esClient, indexName, is.mailboxes);

			is.state = ShardStats.State.OK;
			if (!is.topMailbox.isEmpty()) {
				MailboxStats topMailbox = is.topMailbox.get(0);
				long duration = boxSearchDuration(esClient, topMailbox.mailboxUid);
				is.state = ShardStats.State.ofDuration(duration);
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
		ElasticsearchClient esClient = ESearchActivator.getClient();
		List<String> indexNames = filteredMailspoolIndexNames(esClient);
		logger.debug("indices {} ", indexNames);
		return indexNames.stream() //
				.map(indexName -> indexStats(esClient, indexName, new SimpleShardStats())) //
				.sorted((a, b) -> (int) (b.docCount - a.docCount)).toList();
	}

	private <T extends SimpleShardStats> T indexStats(ElasticsearchClient esClient, String indexName, T is) {
		is.indexName = indexName;
		is.mailboxes = indexMailboxes(esClient, indexName);
		IndicesStats stat;
		try {
			stat = esClient.indices().stats(s -> s.index(indexName)).indices().get(indexName);
			is.size = stat.total().store().sizeInBytes();
			is.docCount = stat.total().docs().count();
			is.deletedCount = stat.total().docs().deleted();
			is.externalRefreshCount = stat.total().refresh().externalTotal();
			is.externalRefreshDuration = stat.total().refresh().externalTotalTimeInMillis();
			return is;
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticIndexException(indexName, e);
		}
	}

	private List<ShardStats.MailboxStats> topMailbox(ElasticsearchClient esClient, String indexName,
			Set<String> mailboxes) {
		try {
			SearchResponse<Void> aggResp = esClient.search(s -> s //
					.index(indexName).size(0)
					.aggregations("countByOwner", a -> a.terms(t -> t.field("owner").size(500))), Void.class);
			return aggResp.aggregations().get("countByOwner").sterms().buckets().array().stream() //
					.map(b -> new ShardStats.MailboxStats(b.key().stringValue(), b.docCount())) //
					.filter(as -> mailboxes.contains(as.mailboxUid)) //
					.toList();
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticIndexException(indexName, e);
		}
	}

	private long boxSearchDuration(ElasticsearchClient esClient, String mailboxUid) {
		try {
			String randomToken = Long.toHexString(Double.doubleToLongBits(Math.random()));
			SearchResponse<Void> results = esClient.search(s -> s //
					.index(getReadIndexAliasName(mailboxUid)) //
					.source(so -> so.fetch(false)) //
					.query(q -> q.bool(b -> b.must(m -> m.hasParent(p -> p //
							.parentType(PARENT_TYPE) //
							.score(false) //
							.query(f -> f.queryString(qs -> qs.query("content:\"" + randomToken + "\"")))))
							.must(m -> m.term(t -> t.field("owner").value(mailboxUid))))),
					Void.class);
			return results.took();
		} catch (ElasticsearchException | IOException e) {
			throw new ElasticDocumentException(getReadIndexAliasName(mailboxUid), e);
		}
	}

	private Set<String> indexMailboxes(ElasticsearchClient esClient, String indexName) {
		try {
			IndexAliases aliasesRsp = esClient.indices().getAlias(a -> a.index(indexName)).get(indexName);
			return aliasesRsp.aliases().keySet().stream().filter(a -> a.startsWith("mailspool_alias_")) //
					.map(a -> a.substring("mailspool_alias_".length())) //
					.collect(Collectors.toSet());
		} catch (Exception e) {
			return Collections.emptySet();
		}
	}

	private static final long TIME_BUDGET = TimeUnit.SECONDS.toNanos(15);

	private SortOrder toSortOrder(SearchSort.Order order) {
		return order == SearchSort.Order.Asc ? SortOrder.Asc : SortOrder.Desc;
	}

	@Override
	public SearchResult searchItems(String domainUid, String dirEntryUid, MailIndexQuery searchQuery) {
		ElasticsearchClient esClient = ESearchActivator.getClient();
		String index = getReadIndexAliasName(dirEntryUid);

		List<SortOptions> sortOptions = (searchQuery.sort != null && searchQuery.sort.hasCriterias())
				? searchQuery.sort.criteria.stream() //
						.map(c -> SortOptions.of(so -> so.field(f -> f.field(c.field).order(toSortOrder(c.order)))))
						.toList()
				: singletonList(SortOptions.of(so -> so.field(f -> f.field("date").order(SortOrder.Desc))));
		Query query = buildEsQuery(searchQuery, dirEntryUid);
		PaginableSearchQueryBuilder paginable = s -> s //
				.source(so -> so.fetch(true)) //
				.trackTotalHits(t -> t.enabled(true)) //
				.query(query) //
				.sort(sortOptions);

		try {
			return (searchQuery.query.offset == 0 && searchQuery.query.maxResults >= Integer.MAX_VALUE)
					? paginatedSearch(esClient, index, dirEntryUid, paginable)
					: simpleSearch(esClient, index, dirEntryUid, paginable, searchQuery.query);
		} catch (Exception e) {
			logger.warn("Failed to search {} ({})", paginable.apply(new SearchRequest.Builder()), e.getMessage());
			return SearchResult.noResult();
		}
	}

	private SearchResult paginatedSearch(ElasticsearchClient esClient, String index, String dirEntryUid,
			PaginableSearchQueryBuilder paginableSearch) throws ElasticsearchException, IOException {
		int deduplicated = 0;
		Map<Integer, InternalMessageSearchResult> results = new LinkedHashMap<>();
		long totalHits = 0;
		int handled = 0;
		try (Pit<ObjectNode> pit = Pit.allocateUsingTimebudget(esClient, index, 60, TIME_BUDGET, ObjectNode.class)) {
			do {
				SearchRequest request = pit.adaptSearch(paginableSearch);
				SearchResponse<ObjectNode> sr = esClient.search(request, ObjectNode.class);
				HitsMetadata<ObjectNode> searchHits = sr.hits();
				if (totalHits == 0) {
					totalHits = searchHits.total().value();
				}
				if (sr.hits() != null && sr.hits().hits() != null) {
					for (Hit<ObjectNode> h : sr.hits().hits()) {
						handled++;
						pit.consumeHit(h);
						deduplicated += handleAndGetDeduplicatedHits(results, h);
					}
				}
			} while (pit.hasNext());
		}

		return createResult(dirEntryUid, totalHits, handled, results, deduplicated);
	}

	private SearchResult simpleSearch(ElasticsearchClient esClient, String index, String dirEntryUid,
			PaginableSearchQueryBuilder paginableSearch, SearchQuery query) throws ElasticsearchException, IOException {
		HitsMetadata<ObjectNode> hits = esClient.search(paginableSearch.andThen(s -> s //
				.index(index).from((int) query.offset).size((int) query.maxResults)), ObjectNode.class).hits();

		int deduplicated = 0;
		Map<Integer, InternalMessageSearchResult> results = new LinkedHashMap<>();
		for (Hit<ObjectNode> sh : hits.hits()) {
			deduplicated += handleAndGetDeduplicatedHits(results, sh);
		}
		return createResult(dirEntryUid, hits.total().value(), hits.hits().size(), results, deduplicated);
	}

	private int handleAndGetDeduplicatedHits(Map<Integer, InternalMessageSearchResult> results, Hit<ObjectNode> sh) {
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

	private Query buildEsQuery(MailIndexQuery query, String entryUid) {
		BoolQuery.Builder bq = QueryBuilders.bool();
		if (query.query.scope.folderScope != null && query.query.scope.folderScope.folderUid != null) {
			if (query.folderUids != null && !query.folderUids.isEmpty()) {
				bq.must(m -> m.bool(b -> {
					query.folderUids.stream()
							.forEach(folder -> b.should(s -> s.term(t -> t.field("in").value(folder))));
					return b.minimumShouldMatch("1")
							.should(s -> s.term(t -> t.field("in").value(query.query.scope.folderScope.folderUid)));
				}));
			} else {
				bq.must(m -> m.term(t -> t.field("in").value(query.query.scope.folderScope.folderUid)));
			}
		}

		bq.must(m -> m.term(t -> t.field("owner").value(entryUid)));
		bq.mustNot(n -> n.term(t -> t.field("is").value("deleted")));
		Operator defaultOperator = query.query.logicalOperator.toString().equals("AND") ? Operator.And : Operator.Or;
		bq = addSearchQuery(bq, query.query.query, defaultOperator);
		bq = addSearchRecordQuery(bq, query.query.recordQuery, defaultOperator);
		bq = addPreciseSearchQuery(bq, "messageId", query.query.messageId);
		bq = addPreciseSearchQuery(bq, "references", query.query.references);

		if (query.query.headerQuery != null && !query.query.headerQuery.query.isEmpty()) {
			List<Query> headerQueries = query.query.headerQuery.query.stream().map(headerQuery -> {
				String queryString = "headers." + headerQuery.name.toLowerCase() + ":\"" + headerQuery.value + "\"";
				return QueryBuilders.queryString(q -> q.query(queryString));
			}).toList();
			Query headerQuery = (query.query.headerQuery.logicalOperator == LogicalOperator.AND) //
					? Queries.and(headerQueries) //
					: Queries.or(headerQueries);
			bq.must(headerQuery);
		}
		return bq.build()._toQuery();
	}

	private BoolQuery.Builder addSearchQuery(BoolQuery.Builder bq, String query, Operator defaultOperator) {
		return (Strings.isNullOrEmpty(query)) //
				? bq //
				: bq.must(m -> m.hasParent(p -> p.parentType(PARENT_TYPE).query(q -> q.queryString(s -> s //
						.query(query).fields(DEFAULT_QUERY_STRING_FIELDS).defaultOperator(defaultOperator)))));
	}

	private BoolQuery.Builder addSearchRecordQuery(BoolQuery.Builder bq, String query, Operator defaultOperator) {
		return (Strings.isNullOrEmpty(query)) //
				? bq //
				: bq.must(m -> m.queryString(s -> s.query(query).defaultOperator(defaultOperator)));
	}

	private BoolQuery.Builder addPreciseSearchQuery(BoolQuery.Builder bq, String searchField, String searchValue) {
		return (searchValue == null) ? bq
				: bq.must(m -> m.hasParent(p -> p.parentType(PARENT_TYPE).query(q -> q //
						.term(t -> t.field(searchField).value(searchValue))).score(false)));
	}

	private Optional<InternalMessageSearchResult> safeResult(Hit<ObjectNode> sh) {
		try {
			return Optional.of(createSearchResult(sh));
		} catch (Exception e) {
			logger.warn("Cannot create result object", e);
			return Optional.empty();
		}
	}

	@SuppressWarnings({ "unchecked" })
	private InternalMessageSearchResult createSearchResult(Hit<ObjectNode> sh) {
		ObjectNode source = sh.source();
		Integer itemId = source.get("itemId") != null ? source.get("itemId").asInt() : null;
		String folderUid = source.get("id").asText().split(":")[0];
		String contUid = "mbox_records_" + folderUid;
		String subject = source.get("subject").asText();
		logger.debug("matching result itemId:{} subject:'{}' in folder:{}", itemId, subject, folderUid);
		int size = source.get("size").asInt();

		JsonNode internalDate = source.get("internalDate");
		ZonedDateTime date = (internalDate != null) //
				? ZonedDateTime.parse(internalDate.asText()) //
				: ZonedDateTime.parse(source.get("date").asText());
		Date messageDate = Date.from(date.toInstant());

		List<String> flags = Streams.stream(source.get("is").elements()).map(JsonNode::asText).toList();
		boolean seen = flags.contains("seen");
		boolean flagged = flags.contains("flagged");

		Map<String, String> headers = Streams.stream(source.get("headers").fields())
				.collect(Collectors.toMap(Entry::getKey, e -> e.getValue().asText()));

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
		boolean hasAttachment = source.get("has") != null && source.get("has").elements().hasNext();

		String preview = source.get("preview") != null ? source.get("preview").asText() : "";

		int imapUid = source.get("uid") != null ? source.get("uid").asInt() : 0;

		return new InternalMessageSearchResult(contUid, itemId, subject, size, "IPM.Note", messageDate, from, to, seen,
				flagged, hasAttachment, preview, imapUid);
	}

	private List<String> filteredMailspoolIndexNames(ElasticsearchClient esClient) {
		try {
			GetMappingResponse response = esClient.indices().getMapping(b -> b.index("mailspool*"));
			return filterMailspoolIndexNames(response);
		} catch (ElasticsearchException | IOException e) {
			return Collections.emptyList();
		}
	}

	private List<String> filterMailspoolIndexNames(GetMappingResponse indexResponse) {
		return indexResponse.result().entrySet().stream() //
				.filter(e -> !e.getKey().startsWith(INDEX_PENDING))
				.filter(e -> e.getValue().mappings().meta() == null
						|| !e.getValue().mappings().meta().containsKey(ESearchActivator.BM_MAINTENANCE_STATE_META_KEY))
				.map(Entry::getKey).sorted().toList();
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
