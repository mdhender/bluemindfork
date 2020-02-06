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

import java.util.Collection;
import java.util.Optional;

import com.google.common.base.MoreObjects;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.directory.hollow.datamodel.consumer.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.consumer.DirectorySearchFactory;
import net.bluemind.directory.hollow.datamodel.consumer.HString;
import net.bluemind.directory.hollow.datamodel.consumer.SerializedDirectorySearch;

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

	@Arguments(required = true, description = "the domain (uid or alias)")
	public String domain;

	@Override
	public void run() {
		CliUtils cli = new CliUtils(ctx);
		String domUid = cli.getDomainUidFromDomain(domain);
		if (domUid == null) {
			throw new ServerFault("domain " + domain + " not found");
		}
		SerializedDirectorySearch hollow = DirectorySearchFactory.get(domUid);
		Collection<AddressBookRecord> bookItems = hollow.all();
		ctx.info("Hollow directory of '" + domUid + "' has " + bookItems.size() + " item(s).");
		for (AddressBookRecord abr : bookItems) {
			ctx.info(stringify(abr));
		}
	}

	private String stringify(AddressBookRecord abr) {
		return MoreObjects.toStringHelper("Rec")//
				.add("uid", hstring(abr.getUid()))//
				.add("displayName", hstring(abr.getName()))//
				.add("email", hstring(abr.getEmail()))//
				.add("dn", hstring(abr.getDistinguishedName()))//
				.toString();
	}

	private String hstring(HString s) {
		return Optional.ofNullable(s).map(HString::getValue).orElse("");
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
