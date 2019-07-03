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
package net.bluemind.lmtp.impl.dto;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class DeliveryReceipt {

	private static final Logger logger = LoggerFactory.getLogger(DeliveryReceipt.class);

	private String folder;
	private Collection<String> flags;
	private boolean successful;
	private String mbox;
	private int uid;

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public Collection<String> getFlags() {
		return flags;
	}

	public void setFlags(Collection<String> flags) {
		this.flags = flags;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	public String getMbox() {
		return mbox;
	}

	public void setMbox(String mbox) {
		this.mbox = mbox;
	}

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public static DeliveryReceipt fromMsg(String msg) {

		logger.debug("LMTP Response {}", msg);
		// LMTP Response 2.1.5 Ok SESSIONID=<cyrus-1468-1480404138-1>
		// [bm.lan!_marketing () 3]
		//
		// mboxName = _marketing
		// domain = bm.lan

		int start = msg.indexOf('[');
		if (start == -1 || !msg.endsWith("]")) {
			logger.warn("No delivery receipt generated for '{}'", msg);
			return null;
		}
		String di = msg.substring(start + 1, msg.length() - 1);
		int mboxEnd = di.lastIndexOf(" (");
		int flagsEnd = di.lastIndexOf(')');
		int domainIdx = di.indexOf('!');
		String domain = di.substring(0, domainIdx);
		String mboxName = di.substring(domainIdx + 1, mboxEnd);
		if (mboxName.startsWith("user.")) {
			mboxName = mboxName.substring(5);
		}
		int folderIdx = mboxName.indexOf('.');
		String folder = "";
		if (folderIdx > 0) {
			folder = mboxName.substring(folderIdx + 1);
			folder = folder.replace('.', '/');
			mboxName = mboxName.substring(0, folderIdx);
		}
		mboxName = mboxName.replace('^', '.');
		String flags = di.substring(mboxEnd + 2, flagsEnd);
		int uid = Integer.parseInt(di.substring(flagsEnd + 2));
		DeliveryReceipt dr = new DeliveryReceipt();
		dr.setMbox(mboxName + "@" + domain);
		dr.setUid(uid);
		if (folder.length() == 0) {
			folder = "INBOX";
		}
		dr.setFolder(folder);
		dr.setSuccessful(true);
		Iterable<String> split = Splitter.on(", ").omitEmptyStrings().split(flags);
		dr.setFlags(Lists.newLinkedList(split));

		logger.info("[{}] folder: '{}', flags: '{}', uid: {}, mbox string {}", dr.getMbox(), folder, dr.getFlags(), uid,
				mboxName);

		return dr;
	}

}
