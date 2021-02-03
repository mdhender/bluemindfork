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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayUtils;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.group.api.gwt.js.JsMember;
import net.bluemind.group.api.gwt.js.JsMemberType;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.admin.client.forms.det.DEDataProvider;
import net.bluemind.ui.admin.client.forms.det.DEPager;
import net.bluemind.ui.admin.client.forms.det.DETable;
import net.bluemind.ui.admin.client.forms.det.SimpleBaseDirEntryFinder;
import net.bluemind.ui.adminconsole.base.DomainsHolder;

public class EditGroupMembers extends CompositeGwtWidgetElement {

	interface EditGroupMembersUiBinder extends UiBinder<DockLayoutPanel, EditGroupMembers> {
	}

	private EditGroupMembersUiBinder binder = GWT.create(EditGroupMembersUiBinder.class);

	public static interface EditGroupConstants extends Constants {
		String addFilter();

		String permsTab();

		String hsmTab();
	}

	private static final EditGroupConstants constants = GWT.create(EditGroupConstants.class);

	public static final String TYPE = "bm.ac.GroupMembersEditor";

	@UiField
	DETable activeMembers;

	@UiField
	TextBox membersFilter;

	@UiField
	DETable membersLookup;

	@UiField
	TextBox lookupFilter;

	@UiField
	PushButton addMembers;

	@UiField
	PushButton rmMembers;

	@UiField
	DEPager lPager;

	@UiField
	DEPager rPager;

	private MembersFinder membersFinder;
	private List<JsMember> toAdd = new LinkedList<>();
	private List<JsMember> toRemove = new LinkedList<>();
	private String groupUid;

	private SimpleBaseDirEntryFinder ugFinder;

	private EditGroupMembersReadOnly delegate;

	private EditGroupMembers(WidgetElement model) {

		if (model.isReadOnly()) {
			delegate = new EditGroupMembersReadOnly();
			initWidget(delegate);
			return;
		}
		DockLayoutPanel dlp = binder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		membersFilter.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					activeMembers.refresh();
				}
			}
		});

		lookupFilter.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					membersLookup.refresh();
				}
			}
		});

		membersFilter.getElement().setAttribute("placeholder", constants.addFilter());
		lookupFilter.getElement().setAttribute("placeholder", constants.addFilter());

		addMembers.getElement().setId("edit-group-add-member");
		rmMembers.getElement().setId("edit-group-remove-member");

		buildMembersTab();
	}

	@UiHandler("addMembers")
	void addMembersClicked(ClickEvent ce) {
		GWT.log("addMembers");
		Set<DirEntry> sel = membersLookup.getSelectedSet();
		if (sel.size() > 0) {
			for (BaseDirEntry de : sel) {
				JsMember m = JsMember.create();
				m.setUid(de.entryUid);
				switch (de.kind) {
				case GROUP:
					m.setType(JsMemberType.group());
					break;
				case EXTERNALUSER:
					m.setType(JsMemberType.external_user());
					break;
				case USER:
					m.setType(JsMemberType.user());
				default:
					break;
				}

				if (contains(toRemove, m)) {
					GWT.log("Remove from toRemove: " + de.entryUid);
					toRemove = remove(toRemove, m);
				} else {
					if (!contains(toAdd, m)) {
						GWT.log("add to toAdd: " + de.entryUid);
						toAdd.add(m);
					}
				}
				GWT.log("toAdd: " + toAdd.size() + ", toRemove: " + toRemove.size());

				membersFinder.addMember(m);
			}
			membersLookup.clearSelection();
			activeMembers.refresh();

			ugFinder.setFilterOut(membersFinder.getMembers());
			membersLookup.refresh();
		}
	}

	@UiHandler("rmMembers")
	void rmMembersClicked(ClickEvent ce) {
		GWT.log("rmMembers");

		Set<DirEntry> sel = activeMembers.getSelectedSet();
		if (sel.size() > 0) {
			for (BaseDirEntry de : sel) {
				JsMember m = JsMember.create();
				m.setUid(de.entryUid);

				switch (de.kind) {
				case GROUP:
					m.setType(JsMemberType.group());
					break;
				case EXTERNALUSER:
					m.setType(JsMemberType.external_user());
					break;
				case USER:
					m.setType(JsMemberType.user());
				default:
					break;
				}

				if (contains(toAdd, m)) {
					GWT.log("Remove from toAdd: " + de.entryUid);
					toAdd = remove(toAdd, m);
				} else {
					if (!contains(toRemove, m)) {
						GWT.log("add to toRemove: " + de.entryUid);
						toRemove.add(m);
					}
				}
				GWT.log("toAdd: " + toAdd.size() + ", toRemove: " + toRemove.size());

				membersFinder.removeMember(m);
			}
			activeMembers.clearSelection();
			activeMembers.refresh();

			ugFinder.setFilterOut(getFilterOut());
			membersLookup.refresh();
		}
	}

	private List<String> getFilterOut() {
		List<String> members = membersFinder.getMembers();
		members.add(groupUid);
		return members;
	}

	private static boolean contains(List<JsMember> members, JsMember m) {
		return members.stream().anyMatch(mm -> mm.getUid().equals(m.getUid()));
	}

	private static List<JsMember> remove(List<JsMember> members, JsMember m) {
		return members.stream().filter(mm -> !mm.getUid().equals(m.getUid())).collect(Collectors.toList());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		if (delegate != null) {
			return;
		}
		JsMapStringJsObject map = model.cast();
		map.put("add-members", JsArrayUtils.readOnlyJsArray(toAdd.toArray(new JsMember[0])));
		map.put("remove-members", JsArrayUtils.readOnlyJsArray(toRemove.toArray(new JsMember[0])));
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		if (delegate != null) {
			delegate.loadModel(model);
			return;
		}
		toAdd.clear();
		toRemove.clear();
		JsMapStringJsObject map = model.cast();
		groupUid = map.getString("groupId");
		JsArray<JsMember> members = map.get("members").cast();

		ugFinder.setDomain(map.getString("domainUid"));

		membersFinder.setDomain(map.getString("domainUid"));

		membersFinder.setMembers(members);

		// FIXME filter out
		ugFinder.setFilterOut(getFilterOut());
		activeMembers.refresh();
		membersLookup.refresh();
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditGroupMembers(e);
			}
		});
	}

	private void buildMembersTab() {
		ugFinder = new UserOrExternalUserSimpleFinder();
		ugFinder.setDomain(DomainsHolder.get().getSelectedDomain().uid);
		membersLookup.initProvider(lookupFilter.asEditor(),
				Arrays.asList(DirEntry.Kind.GROUP, DirEntry.Kind.USER, DirEntry.Kind.EXTERNALUSER), ugFinder);
		rPager.setDisplay(membersLookup);
		rPager.setPageSize(DEDataProvider.PAGE_SIZE);

		membersFinder = new MembersFinder();
		activeMembers.initProvider(membersFilter.asEditor(),
				Arrays.asList(DirEntry.Kind.GROUP, DirEntry.Kind.USER, DirEntry.Kind.EXTERNALUSER), membersFinder);
		lPager.setDisplay(activeMembers);
		lPager.setPageSize(DEDataProvider.PAGE_SIZE);

	}

	@Override
	public void show() {
		if (delegate != null) {
			delegate.show();
			return;
		}
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {

			@Override
			public void execute() {
				activeMembers.onResize();
				membersLookup.onResize();
			}
		});
	}
}
