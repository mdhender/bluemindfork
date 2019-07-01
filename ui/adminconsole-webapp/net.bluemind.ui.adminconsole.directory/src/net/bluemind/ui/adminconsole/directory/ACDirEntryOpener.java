/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.ui.adminconsole.directory;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.directory.api.DirEntry;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.gwtsharing.client.AbstractDirEntryOpener;

public class ACDirEntryOpener extends AbstractDirEntryOpener {

	@Override
	public void open(String domainUid, DirEntry entry) {
		switch (entry.kind) {
		case ADDRESSBOOK: {
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", entry.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editBook", params);
		}
			break;
		case CALENDAR: {
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", entry.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editCalendar", params);
		}
			break;
		case GROUP: {
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", entry.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editGroup", params);
		}
			break;
		case USER: {
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", entry.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editUser", params);
		}
			break;
		case MAILSHARE: {
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", entry.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editMailshare", params);
		}
			break;
		case RESOURCE: {
			Map<String, String> params = new HashMap<>();
			params.put("entryUid", entry.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			Actions.get().showWithParams2("editResource", params);
		}
			break;
		case DOMAIN:
		default:
			break;
		}
	}

}
