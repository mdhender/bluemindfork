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
import java.util.Optional;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IVCardService;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.ExportCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "export", description = "Export an addressbook to an VCF file")
public class ExportAddressBookCommand extends ExportCommand {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("contact");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ExportAddressBookCommand.class;
		}
	}

	@Option(names = "--addressbook-uid", description = "the addressbook uid. , export all addressbooks if not specified")
	public String addressBookUid;

	@Override
	public String getcontainerUid() {
		return addressBookUid;
	}

	@Override
	public String getcontainerType() {
		return IAddressBookUids.TYPE;
	}

	@Override
	public String getFileExtension() {
		return ".vcf";
	}

	@Override
	public void writeFile(File outputFile, String containerUid) {
		String exportedCards = ctx.adminApi().instance(IVCardService.class, containerUid).exportAll();
		if (exportedCards == null) {
			exportedCards = "";
		}
		try {
			Files.write(outputFile.toPath(), exportedCards.getBytes());
		} catch (IOException e) {
			throw new CliException(e);
		}
	}
}
