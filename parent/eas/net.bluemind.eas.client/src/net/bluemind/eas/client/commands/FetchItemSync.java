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

import org.w3c.dom.Element;

import net.bluemind.eas.client.AccountInfos;
import net.bluemind.eas.client.Folder;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.utils.DOMUtils;

/**
 * Performs a Sync AS command for the given folders with 0 as syncKey
 * 
 * 
 */
public class FetchItemSync extends Sync {

	private String serverId;
	private int bodyType;

	public FetchItemSync(Folder f, String serverId, int msEmailBodyType) {
		super(f);
		this.serverId = serverId;
		this.bodyType = msEmailBodyType;
	}

	@Override
	protected void customizeTemplate(AccountInfos ai, OPClient opc) {
		super.customizeTemplate(ai, opc);
		for (Folder f : folders) {
			fetchItem(f);
		}
	}

	// <?xml version="1.0" encoding="UTF-8"?>
	// <Sync>
	// <Collections>
	// <Collection>
	// <SyncKey>9fc07d1c-b4d5-4aaa-b98e-d5797f8a85fc</SyncKey>
	// <CollectionId>3</CollectionId>
	// <GetChanges>0</GetChanges>
	// <Options>
	// <FilterType>2</FilterType>
	// <MIMESupport>2</MIMESupport>
	// <BodyPreference>
	// <Type>4</Type>
	// <TruncationSize>32768</TruncationSize>
	// </BodyPreference>
	// </Options>
	// <Commands>
	// <Fetch>
	// <ServerId>3:106</ServerId>
	// </Fetch>
	// </Commands>
	// </Collection>
	// </Collections>
	// </Sync>
	private void fetchItem(Folder f) {
		Element col = f.getXml();
		DOMUtils.createElementAndText(col, "GetChanges", "0");
		options(f, col);
		Element commands = DOMUtils.createElement(col, "Commands");
		Element fetch = DOMUtils.createElement(commands, "Fetch");
		DOMUtils.createElementAndText(fetch, "ServerId", serverId);
	}

	private void options(Folder f, Element col) {
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
	}

	private void createMailOptions(Element col) {
		// <Options>
		// <FilterType>2</FilterType>
		// <MIMESupport>2</MIMESupport>
		// <BodyPreference>
		// <Type>4</Type>
		// <TruncationSize>32768</TruncationSize>
		// </BodyPreference>
		// </Options>
		Element opts = DOMUtils.createElement(col, "Options");
		DOMUtils.createElementAndText(opts, "FilterType", "2");
		DOMUtils.createElementAndText(opts, "MIMESupport", bodyType == 4 ? "2"
				: "0");

		Element bp = DOMUtils.createElement(opts, "AirSyncBase:BodyPreference");
		DOMUtils.createElementAndText(bp, "AirSyncBase:Type", "" + bodyType);
		DOMUtils.createElementAndText(bp, "AirSyncBase:TruncationSize", "32768");

	}

}
