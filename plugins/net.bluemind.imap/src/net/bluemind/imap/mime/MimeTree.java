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
package net.bluemind.imap.mime;

/**
 * Represents the mime tree of a message. The tree of a message can be obtained
 * by parsing the BODYSTRUCTURE response from the IMAP server.
 * 
 * 
 */
public class MimeTree extends MimePart implements Iterable<MimePart> {

	private int uid;

	public String toString() {
		StringBuilder sb = new StringBuilder();
		printTree(sb, 0, this);
		return sb.toString();
	}

	private void printTree(StringBuilder sb, int depth, MimePart mimeTree) {
		for (int i = 0; i < depth; i++) {
			sb.append("  ");
		}
		if (depth == 0) {
			sb.append("Root[" + uid + "]\n");
		} else {
			sb.append("* " + mimeTree.getAddress() + " " + mimeTree.getMimeType() + "/" + mimeTree.getMimeSubtype());
			if (mimeTree.getBodyParams() != null) {
				sb.append(" " + mimeTree.getBodyParam("name"));
			}
			sb.append("\n");
		}
		for (MimePart mp : mimeTree.children) {
			printTree(sb, depth + 1, mp);
		}
	}

	public boolean isSinglePartMessage() {
		return children.size() == 1 && children.get(0).children.isEmpty();
	}

	public boolean hasAttachments() {
		for (MimePart mp : this) {
			String full = mp.getFullMimeType();
			if (!("text/plain".equals(full)) && !("text/html".equals(full))) {
				return true;
			}
		}
		return false;
	}

	public boolean hasInvitation() {
		for (MimePart mp : this) {
			if (mp.isInvitation()) {
				return true;
			}
		}
		return false;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}
}
