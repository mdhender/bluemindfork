/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cli.filehosting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileType;
import net.bluemind.filehosting.api.IFileHosting;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "list", description = "List content of the filehosting attachment folder")
public class FileHostingListCommand implements ICmdLet, Runnable {

	private CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Parameters(paramLabel = "<login>", description = "user login")
	public String login;

	@Option(names = "--folder", required = false, description = "folder path")
	public String folder;

	@Override
	public void run() {
		ctx.info("login in as user {}", login);
		LoginResponse suResponse = su();

		IFileHosting filehosting = ctx.api(suResponse.authKey).instance(IFileHosting.class,
				suResponse.latd.split("@")[1]);
		String root = folder != null ? folder : "";
		listFolderContent(root, filehosting);
	}

	private void listFolderContent(String path, IFileHosting filehosting) {
		List<FileHostingItem> list = filehosting.list(path);

		List<TblRow> content = new ArrayList<>();
		for (FileHostingItem file : list) {
			if (file.type == FileType.DIRECTORY) {
				listFolderContent(file.path, filehosting);
			} else {
				content.add(new TblRow(file.name, "" + file.size));
			}
		}
		if (content.isEmpty()) {
			if (!path.trim().isEmpty()) {
				ctx.info("Folder {} is empty or does not exist", path);
			}
		} else {
			ctx.info("{}Folder: {}", "\r\n", path);
			ctx.info(AsciiTable.getTable(content, Arrays.asList( //
					new Column().header("Name").dataAlign(HorizontalAlign.LEFT).with(r -> r.value(1)), //
					new Column().header("Filesize").dataAlign(HorizontalAlign.RIGHT).with(r -> r.value(2)))));
		}
	}

	private LoginResponse su() {
		IAuthentication auth = ctx.adminApi().instance(IAuthentication.class);
		return auth.su(login);
	}

	private static class TblRow {
		List<String> columnValues;

		public TblRow(String... values) {
			this.columnValues = new ArrayList<>();
			for (String value : values) {
				columnValues.add(value);
			}
		}

		public String value(int column) {
			return columnValues.get(column - 1);
		}
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("filehosting");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return FileHostingListCommand.class;
		}

	}

}
