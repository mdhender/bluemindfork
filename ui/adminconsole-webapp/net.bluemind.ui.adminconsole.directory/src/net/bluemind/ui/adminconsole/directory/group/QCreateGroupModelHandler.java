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
package net.bluemind.ui.adminconsole.directory.group;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.group.api.gwt.js.JsGroup;
import net.bluemind.group.api.gwt.js.JsMember;
import net.bluemind.group.api.gwt.serder.GroupGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class QCreateGroupModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.QCreateGroupModelHandler";

	private QCreateGroupModelHandler() {
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new QCreateGroupModelHandler();
			}
		});
		GWT.log("bm.ac.QCreateGroupModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {

		GWT.log("initialize QCreateGroupModel");
		JsMapStringJsObject map = model.cast();
		Group ng = new Group();
		map.put("group", new GroupGwtSerDer().serialize(ng).isObject().getJavaScriptObject());
		map.put("members", JsArray.createArray());
		handler.success(null);
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();

		String domainUid = map.getString("domainUid");
		JsGroup group = map.get("group").cast();
		JsArray<JsMember> jMembers = map.get("members").cast();
		final List<Member> members = new ArrayList<>(jMembers.length());
		for (int i = 0; i < jMembers.length(); i++) {
			Member m = new Member();
			m.type = Member.Type.valueOf(jMembers.get(i).getType().value());
			m.uid = jMembers.get(i).getUid();
			members.add(m);
		}
		final GroupGwtEndpoint groups = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
		map.putString("groupUid", uid);
		groups.create(uid, new GroupGwtSerDer().deserialize(new JSONObject(group)),
				new DefaultAsyncHandler<Void>(handler) {

					@Override
					public void success(Void value) {
						groups.add(uid, members, handler);
					}

				});
	}

}
