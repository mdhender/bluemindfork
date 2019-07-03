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
package net.bluemind.ui.im.client.push.handler;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.RosterItemCache;
import net.bluemind.ui.im.client.leftpanel.RosterItem;
import net.bluemind.ui.im.client.push.MessageHandler;

public class RosterEntriesMessageHandler implements MessageHandler<JavaScriptObject> {

	@Override
	public void onMessage(JavaScriptObject message) {
		if (message != null) {

			JSONObject jsonObject = new JSONObject(message);

			JSONArray jsonArray = jsonObject.get("entries").isArray();

			List<RosterItem> entries = new ArrayList<RosterItem>(jsonArray.size());
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject item = jsonArray.get(i).isObject();

				RosterItem ri = new RosterItem();
				ri.userUid = item.get("userUid").isString().stringValue();
				ri.user = item.get("user").isString().stringValue();
				ri.name = item.get("name").isString().stringValue();

				if (item.containsKey("subs")) {
					ri.subs = item.get("subs").isString().stringValue();
				}

				if (item.containsKey("latd")) {
					ri.latd = item.get("latd").isString().stringValue();
				}

				if (item.containsKey("photo")) {
					ri.photo = item.get("photo").isString().stringValue();
				}

				JSONObject jsonPres = item.get("presence").isObject();
				ri.subscriptionType = jsonPres.get("type").isString().stringValue();

				if (jsonPres.containsKey("mode")) {
					ri.mode = jsonPres.get("mode").isString().stringValue();
				}

				if (jsonPres.containsKey("status")) {
					ri.status = jsonPres.get("status").isString().stringValue();
				}

				RosterItemCache.getInstance().put(ri.user, ri);
				entries.add(ri);

			}
			IMCtrl.getInstance().rosterEntries(entries);
		}
	}
}
