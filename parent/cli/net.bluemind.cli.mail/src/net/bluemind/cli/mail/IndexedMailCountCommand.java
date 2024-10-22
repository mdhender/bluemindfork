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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.exception.ElasticIndexException;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.PublicInfos;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "indexed", description = "Shows the number of indexed messages")
public class IndexedMailCountCommand implements ICmdLet, Runnable {
	private static final String INDEX_PENDING_READ_ALIAS = "mailspool_pending_read_alias";

	private CliContext ctx;
	DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	@Option(required = false, names = "--progress", description = "Value indicating the total mails waiting to be indexed")
	public Long progress;

	@Override
	public void run() {
		long docs = 0;
		PublicInfos infos = CliContext.get().adminApi().instance(IInstallation.class).getInfos();
		ctx.info("infos: " + infos.softwareVersion + " " + infos.releaseName);
		ElasticsearchClient esclient = ESearchActivator.getClient();
		do {
			try {
				docs = esclient.indices().stats(s -> s.index(INDEX_PENDING_READ_ALIAS)).all().total().docs().count();
			} catch (ElasticsearchException | IOException e) {
				ctx.error("Stats request fails on index: {}", INDEX_PENDING_READ_ALIAS);
				throw new ElasticIndexException(INDEX_PENDING_READ_ALIAS, e);
			}
			ctx.info("Found " + docs + " indexed mails");
			if (progress != null) {
				double perc = (double) docs / progress * 100;
				ctx.info(df.format(LocalDateTime.now()) + ": Indexed " + docs + " of " + progress + " mails: "
						+ Math.round(perc) + "%");
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					System.exit(0);
				}
			}
		} while (progress != null && docs < progress);
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
			return IndexedMailCountCommand.class;
		}
	}

}
