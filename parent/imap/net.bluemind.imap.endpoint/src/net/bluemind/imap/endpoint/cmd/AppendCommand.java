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

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;

import io.netty.buffer.ByteBuf;

public class AppendCommand extends AbstractFolderNameCommand {

	private static final Pattern append = Pattern.compile("append \"??([^\"\\s]+)([^{]*)", Pattern.CASE_INSENSITIVE);
	private ByteBuf buffer;
	private List<String> flags;
	private Date deliveryDate;

	protected AppendCommand(RawImapCommand raw) {
		super(raw, append, 1);
	}

	public List<String> flags() {
		return flags;
	}

	public Date deliveryDate() {
		return deliveryDate;
	}

	public ByteBuf buffer() {
		return buffer;
	}

	@Override
	protected void folderExtracted(Matcher matcher, FlatCommand flat) {
		String flagsAndDate = matcher.group(2).trim();
		if (flagsAndDate.startsWith("\" ")) {
			flagsAndDate = flagsAndDate.substring(2);
		}

		this.buffer = flat.literals[flat.literals.length - 1];
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
				e.printStackTrace();
			}
		}

	}

}
