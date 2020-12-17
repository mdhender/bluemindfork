/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.index;

import java.util.Optional;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;

import io.vertx.core.json.JsonObject;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailboxQuota;
import picocli.CommandLine.Command;

@Command(name = "info", description = "Get mailbox used quota information")
public class MailboxInfoCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("index");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return MailboxInfoCommand.class;
		}

	}

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) throws Exception {
		if (Strings.isNullOrEmpty(de.value.email)) {
			return;
		}

		JsonObject userJson = new JsonObject();
		userJson.put("email", de.value.email);

		Optional<Long> esQuota = getESQuota(de.uid);
		if (esQuota.isPresent()) {
			userJson.put("ESQuotaKiB", esQuota.get());
		} else {
			userJson.put("ESQuotaKiB", "Failed to fetch ES quota");
		}

		long imapQuota = getImapQuota(domainUid, de);
		userJson.put("IMAPQuotaKiB", imapQuota);

		if (esQuota.isPresent() && imapQuota != 0) {
			userJson.put("ratio", (esQuota.get() / imapQuota * 100));
		}

		ctx.info(userJson.encode());
	}

	private Optional<Long> getESQuota(String mailboxId) {
		try {
			QueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("is", "deleted"));
			SearchResponse sr = ESearchActivator.getClient().prepareSearch(getMailboxAlias(mailboxId)).setQuery(qb)
					.addAggregation(AggregationBuilders.sum("used_quota").field("size")).setSize(0).execute()
					.actionGet();

			InternalSum sum = (InternalSum) sr.getAggregations().get("used_quota");
			return Optional.of((long) sum.getValue() / 1024); // to KiB
		} catch (IndexNotFoundException e) {
			return Optional.empty();
		}
	}

	private String getMailboxAlias(String mailboxId) {
		return "mailspool_alias_" + mailboxId;
	}

	private long getImapQuota(String domainUid, ItemValue<DirEntry> de) {
		IMailboxes iMailboxes = ctx.adminApi().instance(IMailboxes.class, domainUid);
		MailboxQuota quota = iMailboxes.getMailboxQuota(de.uid);

		return quota.used;
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.MAILSHARE, Kind.USER, Kind.GROUP };

	}

}
