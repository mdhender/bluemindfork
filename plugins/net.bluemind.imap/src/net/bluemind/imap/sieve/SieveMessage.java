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
package net.bluemind.imap.sieve;

import java.util.LinkedList;
import java.util.List;

public class SieveMessage {

	private List<String> lines;
	private String responseMessage;

	public SieveMessage() {
		lines = new LinkedList<String>();
	}

	public void addLine(String s) {
		lines.add(s);
	}

	public List<String> getLines() {
		return lines;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append(":\n");
		for (String l : lines) {
			sb.append(l);
			sb.append("\n");
		}
		sb.append(responseMessage);
		return sb.toString();
	}

	public String getResponseMessage() {
		return responseMessage;
	}

	public void setResponseMessage(String line) {
		this.responseMessage = line;

	}

}
