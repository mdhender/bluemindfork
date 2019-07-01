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
package net.bluemind.imap.impl;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.IMAPByteSource;

public class MinaIMAPMessage {

	private List<IMAPByteSource> frags;
	private String messageLine;

	private static final Logger logger = LoggerFactory.getLogger(MinaIMAPMessage.class);

	public MinaIMAPMessage(String line) {
		this.messageLine = line;
		this.frags = new LinkedList<>();
	}

	public void addLine(String s) {
		// will get the ')' after a literal
		if (logger.isDebugEnabled()) {
			logger.info("Adding '{}' to existing message.", s);
		}
		this.messageLine += s;
	}

	public void addBuffer(IMAPByteSource buffer) {
		frags.add(buffer);
	}

	public boolean hasFragments() {
		return !frags.isEmpty();
	}

	public String toString() {
		StringBuilder b = new StringBuilder("\nimap command:");
		b.append(messageLine);
		if (frags != null) {
			for (IMAPByteSource bu : frags) {
				b.append("[buf:").append(bu.size()).append(']');
			}
		}
		return b.toString();
	}

	public List<IMAPByteSource> getFragments() {
		return frags;
	}

	public String getMessageLine() {
		return messageLine;
	}

}
