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
package net.bluemind.cli.todolist;

import java.io.File;
import java.util.Optional;

import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.ExportCommand;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.IVTodo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "export", description = "Export a todolist to an ICS file")
public class ExportTodolistCommand extends ExportCommand {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("todolist");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ExportTodolistCommand.class;
		}
	}

	@Option(names = "--todolistUid", description = "todolist uid, export all todolists if not specified")
	public String todolistUid;

	@Override
	public String getcontainerUid() {
		return todolistUid;
	}

	@Override
	public String getcontainerType() {
		return ITodoUids.TYPE;
	}

	@Override
	public String getFileExtension() {
		return ".ics";
	}

	@Override
	public void writeFile(File outputFile, String containerUid) {
		GenericStream.streamToFile(ctx.adminApi().instance(IVTodo.class, containerUid).exportAll(), outputFile);
	}
}
