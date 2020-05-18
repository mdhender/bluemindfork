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
package net.bluemind.dataprotect.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.directory.api.DirEntry;

/**
 * This class models items that can be restored by DataProtect services.
 * 
 */
@BMApi(version = "3")
public class Restorable {

	public RestorableKind kind;
	public String entryUid;
	private String liveEntryUid;
	public String domainUid;
	public String displayName;

	public static Restorable create(String domainUid, DirEntry d) {
		Restorable ret = new Restorable();

		ret.displayName = d.displayName;
		if (d.email != null && !d.email.trim().isEmpty()) {
			ret.displayName += " (" + d.email + ")";
		}

		switch (d.kind) {
		case USER:
			ret.kind = RestorableKind.USER;
			break;
		case MAILSHARE:
			ret.kind = RestorableKind.MAILSHARE;
			break;
		case ORG_UNIT:
			ret.kind = RestorableKind.OU;
			break;
		case DOMAIN:
			ret.kind = RestorableKind.DOMAIN;
		default:
			throw new RuntimeException("unsupported entry backup " + d.path + " kind " + d.kind);
		}

		ret.domainUid = domainUid;
		ret.entryUid = d.entryUid;

		return ret;
	}

	public void setLiveEntryUid(String liveEntryUid) {
		this.liveEntryUid = liveEntryUid;
	}

	public String liveEntryUid() {
		return liveEntryUid == null ? entryUid : liveEntryUid;
	}
}
