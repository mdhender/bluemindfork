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
package net.bluemind.cli.hollow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.consumer.DirectorySearchFactory;
import net.bluemind.directory.hollow.datamodel.consumer.ListOfEmail;
import net.bluemind.directory.hollow.datamodel.consumer.SerializedDirectorySearch;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "directory", description = "List items in hollow directory")
public class DirectoryDumpCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("hollow");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return DirectoryDumpCommand.class;
		}
	}

	private CliContext ctx;

	@Parameters(paramLabel = "<domain>", description = "the domain (uid or alias)")
	public String domain;

	@Override
	public void run() {
		CliUtils cli = new CliUtils(ctx);
		Optional<String> optDom = cli.getDomainUidByDomainIfPresent(domain);
		String domUid = optDom.orElseGet(() -> {
			ctx.error("domain uid for " + domain + " not found, using " + domain);
			return domain;
		});
		SerializedDirectorySearch hollow = DirectorySearchFactory.get(domUid);
		String version = hollow.root().map(r -> Integer.toString(r.getSequence())).orElse("UNKNOWN");
		Collection<AddressBookRecord> bookItems = hollow.all();
		ctx.info("Hollow directory of '" + domUid + "' has " + bookItems.size() + " item(s) with directory version "
				+ version + ".");
		List<AddressBookRecord> sorted = new ArrayList<>(bookItems);
		sorted.sort((r1, r2) -> Long.compare(r1.getMinimalid(), r2.getMinimalid()));
		for (AddressBookRecord abr : sorted) {
			ctx.info(stringify(abr));
		}
	}

	private String stringify(AddressBookRecord abr) {
		String defMail = hstring(abr.getEmail());
		return MoreObjects.toStringHelper("Rec")//
				.add("uid", hstring(abr.getUid()))//
				.add("minId", abr.getMinimalid())//
				.add("name", abr.getName())//
				.add("email", defMail)//
				.add("otherEmails", emails(abr.getEmails(), defMail))//
				.add("dn", hstring(abr.getDistinguishedName()))//
				.toString();
	}

	private String[] emails(ListOfEmail emails, String filterOut) {
		return emails.stream().map(e -> hstring(e.getAddress())).filter(s -> !s.equals(filterOut))
				.toArray(String[]::new);
	}

	private String hstring(String s) {
		return Strings.nullToEmpty(s);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
