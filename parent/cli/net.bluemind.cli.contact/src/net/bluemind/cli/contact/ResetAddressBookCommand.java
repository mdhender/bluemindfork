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

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ServerFault;

/**
 * This command is here to ensure our that the default maintenance op does
 * nothing
 *
 */
@Command(name = "reset", description = "Reset a addressbook")
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

	@Arguments(required = true, description = "email address")
	public String email;
	
	@Option(name = "--addressbook-uid", description = "the addressbook uid to reset. default is ContactsCollected addressbook")
	public String addressBookUid;
	
	@Option(name = "--dry", description = "Dry-run (do nothing)")
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
		if (!Regex.EMAIL.validate(email)) {
			throw new CliException("Invalid email : " + email);
		}

		String userUid = cliUtils.getUserUidFromEmail(email);

		if(addressBookUid == null) {
			addressBookUid = IAddressBookUids.collectedContactsUserAddressbook(userUid); 
		}
		try {
			if (!dry) {
				ctx.adminApi().instance(IAddressBook.class, addressBookUid).reset();
				System.out.println("Addressbook " + addressBookUid + " of " + email + " was reset.");
			} else {
				System.out.println("DRY : Addressbook " + addressBookUid + " of " + email + " was reset.");
			}
		} catch (ServerFault e) {
			throw new CliException("ERROR resseting addressbook for : " + email, e);
		}
	}

}
