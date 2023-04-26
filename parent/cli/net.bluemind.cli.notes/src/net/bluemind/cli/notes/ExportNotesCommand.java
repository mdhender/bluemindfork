/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.cli.notes;

import java.io.File;
import java.util.Optional;

import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.ExportCommand;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteUids;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "export", description = "Export notes to JSON")
public class ExportNotesCommand extends ExportCommand {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("notes");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ExportNotesCommand.class;
		}
	}

	@Option(names = "--notesUid", description = "notes container uid. Exports all notes containers if not specified")
	public String notesUid;

	@Override
	public String getcontainerUid() {
		return notesUid;
	}

	@Override
	public String getcontainerType() {
		return INoteUids.TYPE;
	}

	@Override
	public String getFileExtension() {
		return ".json";
	}

	@Override
	public void writeFile(File outputFile, String containerUid) {
		Stream stream = ctx.adminApi().instance(INote.class, containerUid).exportAll();
		GenericStream.streamToFile(stream, outputFile);
	}
}
