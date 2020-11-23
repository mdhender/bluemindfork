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

import com.google.common.base.Strings;

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
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;

@Command(name = "import", description = "Import a VCF File to an address book")
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

	@Option(name = "--email", description = "email address")
	public String email;
	
	@Option(required = true, name = "--vcf-file-path", description = "The path of the vcf file.")
	public String vcfFilePath;

	@Option(name = "--addressbook-uid", description = "Target addressbook uid or domainAddressBookUid. Default value for User: default address book of the specified email.")
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
		if (addressBookUid == null && Strings.isNullOrEmpty(email)) {
			ctx.error("At least email or addressbook UID must be present");
			throw new CliException("At least email or addressbook UID must be present");
		}
					
		if (addressBookUid == null && !Strings.isNullOrEmpty(email)) {
			if (!Regex.EMAIL.validate(email)) {
				ctx.error(String.format("Invalid email : %s", email));
				throw new CliException(String.format("Invalid email : %s", email));
			}
			
			try {
				addressBookUid = IAddressBookUids.defaultUserAddressbook(cliUtils.getUserUidFromEmail(email));
			} catch (CliException cli) {
				ctx.error(cli.getMessage());
				throw cli;
			}
		}

		String content = readVcfFileContent();

		try {
			ContainerDescriptor addressBook = ctx.adminApi().instance(IContainers.class).get(addressBookUid);

			if (!dry) {
				ctx.adminApi().instance(IVCardService.class, addressBookUid).importCards(content);
				ctx.info(String.format("VCF file %s was imported into address book %s", vcfFilePath, addressBook));
			} else {
				ctx.info(String.format("DRY: VCF file %s was imported into address book %s", vcfFilePath, addressBook));
			}
		} catch (ServerFault e) {
			if (Strings.isNullOrEmpty(email)) {
				ctx.error(String.format("ERROR importing VCF file %s into addressbook %s: %s", vcfFilePath,
						addressBookUid, e.getMessage()));
				throw new CliException("ERROR importing VCF file " + vcfFilePath + "into addressbook " + addressBookUid,
						e);
			}

			ctx.error(String.format("ERROR importing VCF file %s into addressbook %s of %s: %s", vcfFilePath,
					addressBookUid, email, e.getMessage()));
			throw new CliException(
					"ERROR importing VFC file " + vcfFilePath + " into addressbook " + addressBookUid + " of " + email,
					e);
		}
	}

	private String readVcfFileContent() {
		File file = new File(vcfFilePath);
		if (!file.exists() || file.isDirectory()) {
			ctx.error("File " + vcfFilePath + " not found.");
			throw new CliException("File " + vcfFilePath + " not found.");
		} else {
			try {
				return new String(Files.readAllBytes(Paths.get(vcfFilePath)));
			} catch (IOException e) {
				ctx.error("Unable to read file " + vcfFilePath + ": " + e.getMessage());
				throw new CliException(e);
			}
		}
	}
}
