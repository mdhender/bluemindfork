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
package net.bluemind.ui.adminconsole.directory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.externaluser.api.gwt.endpoint.ExternalUserGwtEndpoint;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroupAsync;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.DoneCancelActionBar;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;

public class EditGroupMembership extends Composite {
	interface EditGroupMembershipUiBinder extends UiBinder<DockLayoutPanel, EditGroupMembership> {

	}

	private static EditGroupMembershipUiBinder uiBinder = GWT.create(EditGroupMembershipUiBinder.class);

	private DialogBox os;
	private DockLayoutPanel dlp;

	@UiField
	DoneCancelActionBar actionBar;

	@UiField
	MyGroupsEntityEdit gEdit;

	private String memberUid;

	private String domainUid;

	private Kind memberKind;

	private EventListener observer;

	public EditGroupMembership(String domainUid, String memberUid, Kind memberKind) {
		this.memberUid = memberUid;
		this.domainUid = domainUid;
		this.memberKind = memberKind;

		this.dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);
		gEdit.setDomain(domainUid);
		actionBar.setDoneAction(new ScheduledCommand() {

			@Override
			public void execute() {
				done();
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {

			@Override
			public void execute() {
				cancel();
			}
		});

		dlp.setHeight("100%");

		load();

	}

	private void load() {
		AsyncHandler<List<ItemValue<Group>>> handler = new AsyncHandler<List<ItemValue<Group>>>() {

			@Override
			public void success(List<ItemValue<Group>> value) {
				ArrayList<DirEntry> entries = new ArrayList<>(value.size());
				for (ItemValue<Group> group : value) {
					entries.add(DirEntry.create(null, null, Kind.GROUP, group.uid, group.displayName, null, false,
							false, false));
				}

				gEdit.setValues(entries);
			}

			@Override
			public void failure(Throwable e) {

			}
		};

		if (memberKind == Kind.USER) {
			new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).memberOf(memberUid, handler);
		} else if (memberKind == Kind.EXTERNALUSER) {
			new ExternalUserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).memberOf(memberUid, handler);
		} else {
			GWT.log("unknown kind of member");
		}
	}

	void cancel() {
		os.hide();
	}

	void done() {
		AsyncHandler<List<ItemValue<Group>>> handler = new AsyncHandler<List<ItemValue<Group>>>() {

			@Override
			public void success(List<ItemValue<Group>> value) {
				doUpdateMembership(value, new DefaultAsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						os.hide();
						EditGroupMembership.this.observer.onBrowserEvent(null);
					}

				});
			}

			@Override
			public void failure(Throwable e) {

			}
		};

		if (memberKind == Kind.USER) {
			new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).memberOf(memberUid, handler);
		} else if (memberKind == Kind.EXTERNALUSER) {
			new ExternalUserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).memberOf(memberUid, handler);
		} else {
			GWT.log("unknown kind of member");
		}
	}

	protected void doUpdateMembership(List<ItemValue<Group>> groups, AsyncHandler<Void> handler) {
		Set<DirEntry> fGroups = gEdit.getValues();
		List<String> currentMembership = new ArrayList<>();
		List<String> askedMembership = new ArrayList<>();

		for (DirEntry entry : fGroups) {
			askedMembership.add(entry.entryUid);
		}

		for (ItemValue<Group> group : groups) {
			currentMembership.add(group.uid);
		}

		List<String> addMembership = new LinkedList<>();
		List<String> removeMembership = new LinkedList<>();

		for (String uid : askedMembership) {
			if (!currentMembership.contains(uid)) {
				addMembership.add(uid);
			}
		}

		for (String uid : currentMembership) {
			if (!askedMembership.contains(uid)) {
				removeMembership.add(uid);
			}
		}

		doRemoveAndAdd(new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid), removeMembership, addMembership,
				handler);
	}

	private void doRemoveAndAdd(final IGroupAsync groupService, final List<String> removeMembership,
			final List<String> addMembership, final AsyncHandler<Void> handler) {

		List<Member> members = null;
		if (memberKind == Kind.USER) {
			members = Arrays.asList(Member.user(memberUid));
		} else if (memberKind == Kind.EXTERNALUSER) {
			members = Arrays.asList(Member.externalUser(memberUid));
		}

		if (!removeMembership.isEmpty()) {
			String uid = removeMembership.remove(0);

			groupService.remove(uid, members, new AsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					doRemoveAndAdd(groupService, removeMembership, addMembership, handler);
				}

				@Override
				public void failure(Throwable e) {
					handler.failure(e);
				}
			});
			return;

		}

		if (!addMembership.isEmpty()) {
			String uid = addMembership.remove(0);

			groupService.add(uid, members, new AsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					doRemoveAndAdd(groupService, removeMembership, addMembership, handler);
				}

				@Override
				public void failure(Throwable e) {
					handler.failure(e);
				}
			});

			return;
		}

		handler.success(null);
	}

	public SizeHint getSizeHint() {
		return new SizeHint(600, 200);

	}

	public void setOverlay(DialogBox os) {
		this.os = os;
	}

	public void registerObserver(EventListener observer) {
		this.observer = observer;

	}

}
