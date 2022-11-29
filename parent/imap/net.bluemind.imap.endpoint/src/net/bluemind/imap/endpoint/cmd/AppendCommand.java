/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.cmd;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import io.netty.buffer.ByteBuf;
import net.bluemind.lib.jutf7.UTF7Converter;

public class AppendCommand extends AnalyzedCommand {

	private static final Logger logger = LoggerFactory.getLogger(AppendCommand.class);
	private ByteBuf buffer;
	private List<String> flags;
	private Date deliveryDate;
	private String folder;

	protected AppendCommand(RawImapCommand raw) {
		super(raw);
		FlatCommand flat = flattenAtoms(false, 0);

		int postFolderStart = -1;
		int lastLiteralStart = flat.fullCmd.lastIndexOf('{');
		this.buffer = flat.literals[flat.literals.length - 1];
		if (flat.literals.length == 1) {
			// folder is given as text
			String utf7Folder;
			int folderStart = "append ".length();
			if (flat.fullCmd.charAt(folderStart) == '"') {
				int endQuote = flat.fullCmd.indexOf('"', folderStart + 2);
				utf7Folder = flat.fullCmd.substring(folderStart + 1, endQuote);
				postFolderStart = endQuote + 2;
			} else {
				int spaceAfterFolder = flat.fullCmd.indexOf(' ', folderStart + 1);
				utf7Folder = flat.fullCmd.substring(folderStart, spaceAfterFolder);
				postFolderStart = spaceAfterFolder + 1;
			}
			folder = UTF7Converter.decode(utf7Folder);
		} else if (flat.literals.length == 2) {
			// folder as atom
			folder = flat.literals[0].toString(StandardCharsets.UTF_8);
			postFolderStart = flat.fullCmd.indexOf("}") + 1;
		}
		String optionalFlagsAndDate = flat.fullCmd.substring(postFolderStart, lastLiteralStart);
		folderExtracted(optionalFlagsAndDate);
	}

	public List<String> flags() {
		return flags;
	}

	public String folder() {
		return folder;
	}

	public Date deliveryDate() {
		return deliveryDate;
	}

	public ByteBuf buffer() {
		return buffer;
	}

	protected void folderExtracted(String flagsAndDate) {

		if (flagsAndDate.startsWith("(")) {
			int end = flagsAndDate.indexOf(')');
			this.flags = Splitter.on(' ').omitEmptyStrings().splitToList(flagsAndDate.substring(1, end));
			flagsAndDate = flagsAndDate.substring(end + 1).trim();
		} else {
			this.flags = Collections.emptyList();
		}

		if (flagsAndDate.startsWith("\"") && flagsAndDate.length() > 10) {
			String unquoted = flagsAndDate.substring(1, flagsAndDate.length() - 1);
			try {
				this.deliveryDate = DateParser.parse(unquoted);
			} catch (ParseException e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

}
