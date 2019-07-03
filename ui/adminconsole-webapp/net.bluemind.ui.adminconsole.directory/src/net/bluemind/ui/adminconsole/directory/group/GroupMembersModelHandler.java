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

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONArray;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.group.api.gwt.js.JsMember;
import net.bluemind.group.api.gwt.serder.MemberGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class GroupMembersModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.GroupMembersModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new GroupMembersModelHandler();
			}
		});
		GWT.log("bm.ac.GroupMembersModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		GWT.log("load group members");
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("groupId");
		String domainUid = map.getString("domainUid");
		GroupGwtEndpoint groups = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

		groups.getMembers(s, new AsyncHandler<List<Member>>() {

			@Override
			public void success(List<Member> value) {
				JsArray<JsMember> members = new GwtSerDerUtils.ListSerDer<Member>(new MemberGwtSerDer())
						.serialize(value).isArray().getJavaScriptObject().cast();

				map.put("members", members);
				handler.success(null);
			}

			@Override
			public void failure(Throwable e) {
				handler.failure(e);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, AsyncHandler<Void> handler) {

		GWT.log("save group members");
		JsMapStringJsObject map = model.cast();
		String s = map.getString("groupId");

		String domainUid = map.getString("domainUid");
		GroupGwtEndpoint groups = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

		JsArray<JsMember> toAdd = map.get("add-members").cast();
		JsArray<JsMember> toRemove = map.get("remove-members").cast();
		doSave(s, groups, toAdd, toRemove, handler);
	}

	private void doSave(final String groupId, final GroupGwtEndpoint groups, JsArray<JsMember> jsToAdd,
			final JsArray<JsMember> toRemove, final AsyncHandler<Void> handler) {

		List<Member> toAdd = new GwtSerDerUtils.ListSerDer<Member>(new MemberGwtSerDer())
				.deserialize(new JSONArray(jsToAdd));
		groups.add(groupId, toAdd, new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				doSaveRemove(groupId, groups, toRemove, handler);
			}

			@Override
			public void failure(Throwable e) {
				handler.failure(e);
			}
		});
	}

	private void doSaveRemove(String groupId, GroupGwtEndpoint groups, JsArray<JsMember> jsToRemove,
			final AsyncHandler<Void> handler) {
		List<Member> toRemove = new GwtSerDerUtils.ListSerDer<Member>(new MemberGwtSerDer())
				.deserialize(new JSONArray(jsToRemove));
		groups.remove(groupId, toRemove, new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				handler.success(null);
			}

			@Override
			public void failure(Throwable e) {
				handler.failure(e);
			}
		});
	}
}
