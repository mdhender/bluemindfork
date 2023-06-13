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
package net.bluemind.cli.mail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.EsBulk;
import net.bluemind.lib.elasticsearch.exception.ElasticBulkException;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.PublicInfos;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "indexreplicated", description = "Index pre-replicated messages")
public class IndexPrereplicatedMailsCommand implements ICmdLet, Runnable {
	private CliContext ctx;
	private static final String TAR = "/var/spool/bm-replication/bodies.replicated.tgz";
	private static final String INDEX_PENDING_READ_ALIAS = "mailspool_pending_read_alias";
	private static final String INDEX_PENDING_WRITE_ALIAS = "mailspool_pending_write_alias";

	@Option(required = false, names = "--progress", description = "Value indicating the total mails waiting to be indexed")
	public Long progress;

	@Override
	public void run() {
		PublicInfos infos = CliContext.get().adminApi().instance(IInstallation.class).getInfos();
		ctx.info("infos: " + infos.softwareVersion + " " + infos.releaseName);

		File file = new File(TAR);
		if (!file.exists()) {
			ctx.info("File " + TAR + " not found");
			System.exit(0);
		}

		try {
			extractAndIndex(file);
		} catch (Exception e) {
			ctx.warn("Cannot extract and index mails:\r\n" + ctx.toStack(e));
		}
	}

	private void extractAndIndex(File file) throws IOException {
		List<IndexedMessageBody> queue = new ArrayList<>();
		AtomicLong counter = new AtomicLong();
		ElasticsearchClient esClient = ESearchActivator.getClient();
		Consumer<IndexedMessageBody> consumer = msg -> {
			queue.add(msg);
			if (queue.size() == 100) {
				long current = counter.addAndGet(100);
				ctx.info("Indexed " + current + " mailbodies");
				index(esClient, queue);
				queue.clear();
			}
		};
		Set<String> indexed = getIndexedUids(esClient);
		ctx.info(indexed.size() + " mails have already been indexed.");
		try (InputStream fi = Files.newInputStream(file.toPath());
				InputStream bi = new BufferedInputStream(fi);
				InputStream gzi = new GzipCompressorInputStream(bi);
				ArchiveInputStream archiveStream = new TarArchiveInputStream(gzi)) {
			extract(archiveStream, consumer, indexed);
		}
		long current = counter.addAndGet(queue.size());
		ctx.info("Indexed " + current + " mailbodies");
		index(esClient, queue);
	}

	private void extract(ArchiveInputStream archiveStream, Consumer<IndexedMessageBody> consumer, Set<String> indexed)
			throws IOException {
		ArchiveEntry entry = null;
		while ((entry = archiveStream.getNextEntry()) != null) {
			if (indexed.contains(uidFromFileName(entry.getName()))) {
				continue;
			}

			try {
				String json = new String(IOUtils.toByteArray(archiveStream), StandardCharsets.UTF_8);
				IndexedMessageBody asMsgBody = IndexedMessageBody.fromJson(json);
				consumer.accept(asMsgBody);
			} catch (Exception e) {
				ctx.info("Cannot handle file {} --> {}", entry.getName(), e.getMessage());
			}
		}

	}

	private String uidFromFileName(String name) {
		return name.substring(0, name.indexOf('.'));
	}

	private Set<String> getIndexedUids(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
		Set<String> uids = new HashSet<>();
		ResponseBody<Void> response = esClient.search(s -> s.index(INDEX_PENDING_READ_ALIAS) //
				.query(MatchAllQuery.of(q -> q)._toQuery()) //
				.source(src -> src.fetch(false)) //
				.scroll(t -> t.time("180s")) //
				.size(10000), Void.class);

		long current = 0;
		while (current < response.hits().total().value()) {
			response.hits().hits().stream().forEach(hit -> uids.add(hit.id()));
			current += response.hits().hits().size();
			String scrollId = response.scrollId();
			if (current < response.hits().total().value()) {
				response = esClient.scroll(s -> s.scrollId(scrollId).scroll(t -> t.time("180s")), Void.class);
			}
		}

		String scrollId = response.scrollId();
		esClient.clearScroll(c -> c.scrollId(scrollId));

		return uids;
	}

	private void index(ElasticsearchClient esClient, List<IndexedMessageBody> bodies) {
		try {
			new EsBulk(esClient).commitAll(bodies, (body, o) -> o.index(i -> i //
					.index(INDEX_PENDING_WRITE_ALIAS) //
					.id(body.uid) //
					.document(body.toMap()))).ifPresent(this::reportErrors);
		} catch (ElasticBulkException e) {
			ctx.error("Bulk request fails on index: {} while indexing {} bodies", INDEX_PENDING_WRITE_ALIAS,
					bodies.size(), e);
		}
	}

	private void reportErrors(BulkResponse response) {
		if (!response.errors()) {
			return;
		}
		List<BulkResponseItem> failedItems = response.items().stream().filter(i -> i.error() != null).toList();
		failedItems.forEach(i -> ctx.error("Bulk request failed on item id:{} error:{} stack:{}", //
				i.id(), i.error().type(), i.error().stackTrace()));
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mail");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return IndexPrereplicatedMailsCommand.class;
		}
	}

}
