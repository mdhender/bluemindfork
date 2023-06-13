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

import java.io.IOException;
import java.util.Optional;

import com.google.common.base.Strings;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchResponse;
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
		SearchResponse<Void> response;
		try {
			response = ESearchActivator.getClient().search(s -> s //
					.size(0) //
					.index(getMailboxAlias(mailboxId)) //
					.query(q -> q.bool(b -> b.mustNot(mn -> mn.term(t -> t.field("is").value("deleted"))))) //
					.aggregations("used_quota", a -> a.sum(sum -> sum.field("size"))), Void.class);

			double sum = response.aggregations().get("used_quota").sum().value();
			return Optional.of((long) sum / 1024); // to KiB
		} catch (ElasticsearchException | IOException e) {
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
