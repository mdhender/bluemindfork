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
package net.bluemind.ui.adminconsole.directory.user;

import java.util.Arrays;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.directory.api.gwt.js.JsBaseDirEntryAccountType;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;
import net.bluemind.user.api.gwt.js.JsUser;
import net.bluemind.user.api.gwt.serder.UserGwtSerDer;

public class QCreateUserModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.QCreateUserModelHandler";

	protected QCreateUserModelHandler() {
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new QCreateUserModelHandler();
			}
		});
		GWT.log("bm.ac.QCreateUserModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {

		GWT.log("initialize QCreateUserModel");
		JsMapStringJsObject map = model.cast();

		if (DomainsHolder.get().getSelectedDomain() != null) {
			map.putString("domainUid", DomainsHolder.get().getSelectedDomain().uid);
		}
		map.put("user", JsUser.create());
		handler.success(null);

	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();

		JsItemValue<JsDomain> domain = map.get("domain").cast();
		if (domain == null || domain.getValue().getGlobal()) {
			handler.failure(new RuntimeException(ErrorCodeTexts.INST.getString("NOT_IN_GLOBAL_DOMAIN")));
			return;
		}
		final String domainUid = map.getString("domainUid");
		JsUser user = map.get("user").cast();
		user.setArchived(false);
		user.setSystem(false);

		String accountType = map.getString("accountType");
		if (accountType == null || accountType.isEmpty() || "FULL".equals(accountType)) {
			user.setAccountType(JsBaseDirEntryAccountType.FULL());
		} else if ("SIMPLE".equals(accountType)) {
			user.setAccountType(JsBaseDirEntryAccountType.SIMPLE());
		} else if ("VISIO".equals(accountType)) {
			user.setAccountType(JsBaseDirEntryAccountType.FULL_AND_VISIO());
		}

		UserGwtEndpoint users = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
		map.putString("userId", uid);
		users.create(uid, new UserGwtSerDer().deserialize(new JSONObject(user)),
				new DefaultAsyncHandler<Void>(handler) {

					@Override
					public void success(Void value) {
						String groupUid = map.getString("defaultGroup");
						if (groupUid == null) {
							handler.success(null);
							return;
						}
						GroupGwtEndpoint groups = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
						groups.add(groupUid, Arrays.asList(Member.user(uid)), handler);
					}
				});
	}

}
