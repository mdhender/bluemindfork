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
package net.bluemind.cli.contact;

import java.util.Optional;

import com.google.common.base.Strings;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * This command is here to ensure our that the default maintenance op does
 * nothing
 *
 */
@Command(name = "reset", description = "Reset an addressbook")
public class ResetAddressBookCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("contact");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ResetAddressBookCommand.class;
		}

	}

	@Option(names = "--email", description = "email address")
	public String email;

	@Option(names = "--addressbook-uid", description = "The addressbook uid to reset. Default is ContactsCollected addressbook of the specified email. Default DomainAddressBook is domainAddressbook uid.")
	public String addressBookUid;

	@Option(names = "--dry", description = "Dry-run (do nothing)")
	public boolean dry = false;

	private CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {
		if (addressBookUid == null && Strings.isNullOrEmpty(email)) {
			ctx.error("At least email or address book UID must be present");
			throw new CliException("At least email or address book UID must be present");
		}

		if (addressBookUid == null && !Strings.isNullOrEmpty(email)) {
			if (!Regex.EMAIL.validate(email)) {
				ctx.error(String.format("Invalid email : %s", email));
				throw new CliException(String.format("Invalid email : %s", email));
			}

			try {
				addressBookUid = IAddressBookUids.collectedContactsUserAddressbook(cliUtils.getUserUidByEmail(email));
			} catch (CliException cli) {
				ctx.error(cli.getMessage());
				throw cli;
			}
		}

		try {
			ContainerDescriptor addressBook = ctx.adminApi().instance(IContainers.class).get(addressBookUid);

			if (!dry) {
				ctx.adminApi().instance(IAddressBook.class, addressBookUid).reset();
				ctx.info(String.format("Addressbook %s was reset.", addressBook));
			} else {
				ctx.info(String.format("DRY: Addressbook %s was reset.", addressBook));
			}
		} catch (ServerFault e) {
			if (Strings.isNullOrEmpty(email)) {
				ctx.error(String.format("ERROR reseting addressbook %s: %s", addressBookUid, e.getMessage()));
				throw new CliException("ERROR reseting addressbook " + addressBookUid, e);
			}

			ctx.error(String.format("ERROR reseting addressbook %s of %s: %s", addressBookUid, email, e.getMessage()));
			throw new CliException("ERROR reseting addressbook " + addressBookUid + " of " + email, e);
		}
	}

}
