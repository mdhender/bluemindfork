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
package net.bluemind.tag.api;

// FIXME should not be here.. tags != server tags
public enum TagDescriptor {
	bm_redirector("bm/redirector"), //
	bm_settings("bm/settings"), //
	bm_webmail("bm/webmail"), //
	bm_calendar("bm/cal"), //
	mail_smtp("mail/smtp"), //
	bm_es("bm/es"), //
	mail_imap("mail/imap"), //
	bm_core("bm/core"), bm_ac("bm/ac"), //
	bm_hps("bm/hps"), //
	bm_nginx("bm/nginx"), //
	bm_nginx_edge("bm/nginx-edge"), //
	bm_xmpp("bm/xmpp"), //
	bm_pgsql("bm/pgsql"), //
	bm_contact("bm/contact"), //
	mail_smtp_edge("mail/smtp-edge"), //
	bm_filehosting("filehosting/data");

	private final String tag;

	private TagDescriptor(String tag) {
		this.tag = tag;
	}

	public String getTag() {
		return tag;
	}

	public static boolean contains(String otherTag) {
		for (TagDescriptor tag : TagDescriptor.values()) {
			if (tag.getTag().equals(otherTag)) {
				return true;
			}
		}
		return false;
	}
}
