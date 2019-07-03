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
package net.bluemind.eas.client.commands;

import java.util.List;

import org.w3c.dom.Element;

import net.bluemind.eas.client.AccountInfos;
import net.bluemind.eas.client.Folder;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.utils.DOMUtils;

/**
 * Performs a Sync AS command for the given folders. Defaults to no pagination
 * at all
 * 
 * 
 */
public class GetChangesSync extends Sync {

	private int pageSize;
	private boolean asHTML;
	private List<Long> toFlag;

	public GetChangesSync(Folder... folders) {
		super(folders);
		this.pageSize = Integer.MAX_VALUE;
	}

	@Override
	protected void customizeTemplate(AccountInfos ai, OPClient opc) {
		super.customizeTemplate(ai, opc);
		for (Folder f : folders) {
			getChanges(f);
		}
	}

	private void getChanges(Folder f) {
		Element col = f.getXml();
		DOMUtils.createElement(col, "GetChanges");
		DOMUtils.createElementAndText(col, "WindowSize", "" + pageSize);
		switch (f.getType()) {
		case DEFAULT_INBOX_FOLDER:
			createMailOptions(col);
			break;
		case DEFAULT_CALENDAR_FOLDER:
			break;
		case DEFAULT_CONTACTS_FOLDER:
			break;
		case DEFAULT_DELETED_ITEMS_FOLDERS:
			break;
		case DEFAULT_DRAFTS_FOLDERS:
			break;
		case DEFAULT_JOURNAL_FOLDER:
			break;
		case DEFAULT_NOTES_FOLDER:
			break;
		case DEFAULT_OUTBOX_FOLDER:
			break;
		case DEFAULT_SENT_EMAIL_FOLDER:
			break;
		case DEFAULT_TASKS_FOLDER:
			break;
		case UNKNOWN_FOLDER_TYPE:
			break;
		case USER_CREATED_CALENDAR_FOLDER:
			break;
		case USER_CREATED_CONTACTS_FOLDER:
			break;
		case USER_CREATED_EMAIL_FOLDER:
			break;
		case USER_CREATED_JOURNAL_FOLDER:
			break;
		case USER_CREATED_NOTES_FOLDER:
			break;
		case USER_CREATED_TASKS_FOLDER:
			break;
		case USER_FOLDER_GENERIC:
			break;

		}
		if (toFlag != null) {
			Element commands = DOMUtils.createElement(col, "Commands");
			for (Long l : toFlag) {
				Element flag = DOMUtils.createElement(commands, "Change");
				DOMUtils.createElementAndText(flag, "ServerId", f.getServerId()
						+ ":" + l);
				Element appData = DOMUtils.createElement(flag,
						"ApplicationData");
				Element ef = DOMUtils.createElement(appData, "Email:Flag");
				DOMUtils.createElementAndText(ef, "Email:FlagStatus", "1");
			}
		}
	}

	private void createMailOptions(Element col) {
		// <Options>
		// <FilterType>2</FilterType>
		// <MIMETruncation>1</MIMETruncation>
		// <MIMESupport>0</MIMESupport>
		// <BodyPreference>
		// <Type>1</Type>
		// <TruncationSize>500</TruncationSize>
		// </BodyPreference>
		// </Options>
		Element opts = DOMUtils.createElement(col, "Options");
		DOMUtils.createElementAndText(opts, "FilterType", "2");
		DOMUtils.createElementAndText(opts, "MIMETruncation", "1");
		DOMUtils.createElementAndText(opts, "MIMESupport", "0");

		Element bp = DOMUtils.createElement(opts, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bp, "AirSyncBase:Type", asHTML ? "2"
				: "1");
		DOMUtils.createElementAndText(bp, "AirSyncBase:TruncationSize",
				asHTML ? "5120" : "500");

	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public void setHtml(boolean asHTML) {
		this.asHTML = true;
	}

	public void setToFlag(List<Long> toFlag) {
		this.toFlag = toFlag;
	}

}
