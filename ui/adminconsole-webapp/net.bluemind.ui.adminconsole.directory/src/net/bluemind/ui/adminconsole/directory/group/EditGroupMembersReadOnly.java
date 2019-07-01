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

import java.util.Arrays;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.group.api.gwt.js.JsMember;
import net.bluemind.ui.admin.client.forms.det.DEPager;
import net.bluemind.ui.admin.client.forms.det.DETable;

public class EditGroupMembersReadOnly extends Composite {

	interface EditGroupMembersUiBinder extends UiBinder<DockLayoutPanel, EditGroupMembersReadOnly> {
	}

	private EditGroupMembersUiBinder binder = GWT.create(EditGroupMembersUiBinder.class);

	public static final String TYPE = "bm.ac.GroupMembersEditor";

	@UiField
	TextBox membersFilter;

	@UiField
	DETable activeMembers;

	@UiField
	DEPager lPager;

	private MembersFinder membersFinder;

	public EditGroupMembersReadOnly() {

		DockLayoutPanel dlp = binder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		buildMembersTab();

	}

	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsArray<JsMember> members = map.get("members").cast();

		membersFinder.setDomain(map.getString("domainUid"));

		membersFinder.setMembers(members);

		activeMembers.refresh();
	}

	private void buildMembersTab() {
		membersFinder = new MembersFinder();
		activeMembers.initProvider(
				membersFilter.asEditor(), Arrays.asList(DirEntry.Kind.GROUP,
						DirEntry.Kind.USER, DirEntry.Kind.EXTERNALUSER),
				membersFinder);
		lPager.setDisplay(activeMembers);
		lPager.setPageSize(25);

	}

	public void show() {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {

			@Override
			public void execute() {
				activeMembers.onResize();
			}
		});
	}
}
