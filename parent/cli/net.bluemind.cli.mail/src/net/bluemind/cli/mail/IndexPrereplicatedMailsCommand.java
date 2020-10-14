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
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHit;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.PublicInfos;

@Command(name = "indexreplicated", description = "Index pre-replicated messages")
public class IndexPrereplicatedMailsCommand implements ICmdLet, Runnable {
	private CliContext ctx;
	public static final String tar = "/var/spool/bm-replication/bodies.replicated.tgz";
	private static final String PENDING_TYPE = "eml";
	public static final String INDEX_PENDING = "mailspool_pending";

	@Option(required = false, name = "--progress", description = "Value indicating the total mails waiting to be indexed")
	public Long progress;

	@Override
	public void run() {
		PublicInfos infos = CliContext.get().adminApi().instance(IInstallation.class).getInfos();
		ctx.info("infos: " + infos.softwareVersion + " " + infos.releaseName);

		File file = new File(tar);
		if (!file.exists()) {
			ctx.info("File " + tar + " not found");
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
		Consumer<IndexedMessageBody> consumer = msg -> {
			queue.add(msg);
			if (queue.size() == 100) {
				long current = counter.addAndGet(100);
				ctx.info("Indexed " + current + " mailbodies");
				index(queue);
				queue.clear();
			}
		};
		Set<String> indexed = getIndexedUids();
		ctx.info(indexed.size() + " mails have already been indexed.");
		try (InputStream fi = Files.newInputStream(file.toPath());
				InputStream bi = new BufferedInputStream(fi);
				InputStream gzi = new GzipCompressorInputStream(bi);
				ArchiveInputStream archiveStream = new TarArchiveInputStream(gzi)) {
			extract(archiveStream, consumer, indexed);
		}
		long current = counter.addAndGet(queue.size());
		ctx.info("Indexed " + current + " mailbodies");
		index(queue);
	}

	private void extract(ArchiveInputStream archiveStream, Consumer<IndexedMessageBody> consumer, Set<String> indexed)
			throws IOException {
		ArchiveEntry entry = null;
		while ((entry = archiveStream.getNextEntry()) != null) {
			if (indexed.contains(uidFromFileName(entry.getName()))) {
				continue;
			}

			String json = new String(IOUtils.toByteArray(archiveStream), StandardCharsets.UTF_8);
			IndexedMessageBody asMsgBody = IndexedMessageBody.fromJson(json);
			consumer.accept(asMsgBody);
		}

	}

	private String uidFromFileName(String name) {
		return name.substring(0, name.indexOf('.'));
	}

	private Set<String> getIndexedUids() {
		Set<String> uids = new HashSet<>();
		Client client = ESearchActivator.getClient();
		SearchResponse r = client.prepareSearch(INDEX_PENDING).setQuery(new MatchAllQueryBuilder())
				.setFetchSource(false).setScroll(TimeValue.timeValueSeconds(180)).setTypes(PENDING_TYPE).setSize(10000)
				.execute().actionGet();

		long current = 0;
		while (current < r.getHits().getTotalHits()) {
			for (SearchHit h : r.getHits().getHits()) {
				uids.add(h.getId());
				current++;
			}

			if (current < r.getHits().getTotalHits()) {
				r = client.prepareSearchScroll(r.getScrollId()).setScroll(TimeValue.timeValueSeconds(180)).execute()
						.actionGet();
			}

		}

		return uids;
	}

	private void index(List<IndexedMessageBody> bodies) {
		Client client = ESearchActivator.getClient();
		EsBulk bulkOp = startBulk();
		for (IndexedMessageBody body : bodies) {
			IndexRequestBuilder request = client.prepareIndex(INDEX_PENDING, PENDING_TYPE).setId(body.uid)
					.setSource(body.toMap());
			bulkOp.bulk.add(request);
		}
		bulkOp.commit();
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

	private static class EsBulk {

		private BulkRequestBuilder bulk;

		public EsBulk(BulkRequestBuilder bulk) {
			this.bulk = bulk;
		}

		public void commit() {
			if (bulk.numberOfActions() > 0) {
				bulk.execute().actionGet();
			}
		}

	}

	public EsBulk startBulk() {
		Client client = ESearchActivator.getClient();
		return new EsBulk(client.prepareBulk());
	}

}
