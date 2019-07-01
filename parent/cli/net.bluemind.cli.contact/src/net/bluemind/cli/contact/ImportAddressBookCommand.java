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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IVCardService;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ServerFault;

@Command(name = "import", description = "import an VCF File")
public class ImportAddressBookCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("contact");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ImportAddressBookCommand.class;
		}

	}

	@Arguments(required = true, description = "email address")
	public String email;
	
	@Option(required = true, name = "--vcf-file-path", description = "The path of the vcf file.")
	public String vcfFilePath;
	
	@Option(name = "--addressbook-uid", description = "Target addressbook uid. Default value: default addressBook")
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

		String content = null;
		File file = new File(vcfFilePath);
		if (!file.exists() || file.isDirectory()) {
			throw new CliException("File " + vcfFilePath + " not found.");
		} else {
			try {
				content = new String(Files.readAllBytes(Paths.get(vcfFilePath)));	
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}

		if(addressBookUid == null) {
			addressBookUid = IAddressBookUids.defaultUserAddressbook(userUid); 
		}

		try{
			if (!dry) {
				ctx.adminApi().instance(IVCardService.class, addressBookUid).importCards(content);
				ctx.info("AddressBook " + addressBookUid + " of " + email + " was imported");
			} else {
				ctx.info("DRY : AddressBook " + addressBookUid + " of " + email + " was imported");
			}
		} catch (ServerFault e) {
			throw new CliException("ERROR importing addressbook for : " + email + " : ", e);
		}			
	}	
}
