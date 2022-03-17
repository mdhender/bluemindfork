/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.imap.command;

public class CommandArgument {

	private String commandString;
	private byte[] literalData;

	public CommandArgument(String s, byte[] literalData) {
		this.commandString = s;
		this.literalData = literalData;
	}

	public String getCommandString() {
		return commandString;
	}

	public void setCommandString(String s) {
		this.commandString = s;
	}

	public byte[] getLiteralData() {
		return literalData;
	}

	public void setLiteralData(byte[] literalData) {
		this.literalData = literalData;
	}

	public boolean hasLiteralData() {
		return literalData != null;
	}

}
