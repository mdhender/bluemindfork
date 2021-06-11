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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cli.mail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.freva.asciitable.AsciiTable;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.cli.utils.CliUtils.ResolvedMailbox;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.mailbox.api.Mailbox.Type;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "unexpunge", description = "Recover deleted emails into their original mailbox")
public class UnexpungeCommand implements ICmdLet, Runnable {

	@Option(names = "--folder", description = "Search deleted items in the given folder, defaults to INBOX (eg. 'Sub/Fol/Der')")
	public String folder;

	@Option(names = "--days", description = "Recover all deleted messages in the last X days")
	public int days = 0;

	@Option(names = "--id", description = "The id of the mail to recover")
	public long id = 0;

	@Option(names = "--dry", description = "Do not run the recovery for real")
	public boolean dry = false;

	@Parameters(paramLabel = "<email_address>", description = "email address (might be an alias) of the mailbox to recover from")
	public String target;

	@Option(names = "--authn", description = "When recovering from a mailshare, you need to authenticate as someone with perms on the shared folder")
	public String authN;

	private CliContext ctx;

	@Override
	public void run() {
		CliUtils cu = new CliUtils(ctx);
		ResolvedMailbox resolved = cu.getMailboxByEmail(target);
		if (resolved.mailbox.value.type != Type.user && authN == null) {
			throw new CliException("To recover from " + resolved.mailbox
					+ " you need to specify --authn with the alias of a user with write perms on the mailbox");
		}

		if (resolved.mailbox.value.type == Type.user) {
			authN = resolved.mailbox.value.name + "@" + resolved.domainUid;
		} else {
			ResolvedMailbox resolveAuth = cu.getMailboxByEmail(authN);
			if (resolveAuth.mailbox.value.type != Type.user) {
				throw new CliException("--authn must match a valid user alias");
			}
			authN = resolveAuth.mailbox.value.name + "@" + resolveAuth.domainUid;
		}
		ctx.info("authN resolves to " + authN);
		IAuthentication authApi = ctx.adminApi().instance(IAuthentication.class);
		LoginResponse sudo = authApi.su(authN);
		if (sudo.status != Status.Ok) {
			throw new CliException("Sudo as " + authN + " failed: " + sudo.status + " " + sudo.message);
		}

		IServiceProvider userProv = ctx.api(sudo.authKey);
		IAuthentication userAuthApi = userProv.instance(IAuthentication.class);

		CyrusPartition partition = CyrusPartition.forServerAndDomain(resolved.mailbox.value.dataLocation,
				resolved.domainUid);
		String partForApi = resolved.domainUid.replace('.', '_');
		String mboxForApi = resolved.mailbox.value.type.nsPrefix + resolved.mailbox.value.name.replace('.', '^');
		IMailboxFolders folders = userProv.instance(IMailboxFolders.class, partForApi, mboxForApi);
		String fn = Optional.ofNullable(folder)
				.orElseGet(() -> resolved.mailbox.value.type.sharedNs ? resolved.mailbox.value.name : "INBOX");
		ctx.info("Partition is " + partition + ", checked folder is '" + fn + "'");
		ItemValue<MailboxFolder> located = folders.byName(fn);

		if (located == null) {
			ctx.info("Could not locate folder '" + folder + "', available folders are:");
			folders.all().forEach(c -> ctx.info(" * " + c.value.fullName));
		} else {
			ctx.info("Located folder is " + located);
			IMailboxItems itemsApi = userProv.instance(IMailboxItems.class, located.uid);
			ItemFlagFilter deletedFilter = ItemFlagFilter.create().must(ItemFlag.Deleted);
			Count deleted = itemsApi.count(deletedFilter);
			ctx.info("Folder has " + deleted.total + " deleted message(s)");

			if (deleted.total > 0) {
				if (days == 0 && id == 0) {
					showDeletedMessages(itemsApi, mi -> true, mi -> {
					});
				} else if (days > 0) {
					ctx.info("Recovering messages less than " + days + " day(s)) old");
					long now = System.currentTimeMillis();
					Predicate<ItemValue<MailboxItem>> pred = mi -> TimeUnit.MILLISECONDS
							.toDays(now - mi.updated.getTime()) < days;
					showDeletedMessages(itemsApi, pred, new Unexpunger(ctx, itemsApi));
				} else if (id > 0) {
					ctx.info("Recover message with id " + id);
					if (!dry) {
						ItemIdentifier ack = itemsApi.unexpunge(id);
						ctx.info("ack: " + ack.version);
					} else {
						ctx.info("dry mode is ON.");
					}
				}
			}
		}

		userAuthApi.logout();

	}

	private void showDeletedMessages(IMailboxItems itemsApi, Predicate<ItemValue<MailboxItem>> filter,
			Consumer<ItemValue<MailboxItem>> cons) {
		long cv = itemsApi.getVersion();
		int start = 0;
		int page = 25;

		ListResult<Long> chunk = itemsApi.allIds("+deleted", cv, page, start);
		long fetched = 0;
		List<ItemValue<MailboxItem>> toRestore = new LinkedList<>();
		while (!chunk.values.isEmpty()) {
			toRestore.addAll(showChunk(itemsApi, chunk, filter));

			start += chunk.values.size();
			fetched += chunk.values.size();
			chunk = itemsApi.allIds("+deleted", cv, page, start);
		}
		ctx.info("Checked " + fetched + " deleted item(s), will restore " + toRestore.size() + " item(s)");
		toRestore.forEach(cons);
	}

	private List<ItemValue<MailboxItem>> showChunk(IMailboxItems itemsApi, ListResult<Long> chunk,
			Predicate<ItemValue<MailboxItem>> filter) {
		List<ItemValue<MailboxItem>> mget = itemsApi.multipleById(chunk.values);
		String[] headers = { "id", "subject", "preview", "last-modification" };
		int chunkSize = mget.size();
		List<String[]> data = new ArrayList<>(chunkSize);
		List<ItemValue<MailboxItem>> ret = new ArrayList<>(chunkSize);
		for (int i = 0; i < chunkSize; i++) {
			ItemValue<MailboxItem> item = mget.get(i);
			if (filter.test(item)) {
				String[] dataRow = new String[headers.length];
				dataRow[0] = Long.toString(item.internalId);
				dataRow[1] = item.value.body.subject;
				dataRow[2] = item.value.body.preview;
				dataRow[3] = item.updated.toString();
				data.add(dataRow);
				if (!dry) {
					ret.add(item);
				} else {
					ctx.info("Skipping action on " + item + " because dry mode is enabled.");
				}
			}
		}
		if (!data.isEmpty()) {
			String[][] forDisplay = new String[data.size()][];
			forDisplay = data.toArray(forDisplay);
			ctx.info(AsciiTable.getTable(headers, forDisplay));
		}
		return ret;
	}

	public static class Unexpunger implements Consumer<ItemValue<MailboxItem>> {

		private CliContext ctx;
		private IMailboxItems itemsApi;

		public Unexpunger(CliContext ctx, IMailboxItems itemsApi) {
			this.ctx = ctx;
			this.itemsApi = itemsApi;
		}

		@Override
		public void accept(ItemValue<MailboxItem> t) {
			ItemIdentifier unexp = itemsApi.unexpunge(t.internalId);
			ctx.info("ack received: " + unexp.version);
		}

	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mail");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UnexpungeCommand.class;
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
