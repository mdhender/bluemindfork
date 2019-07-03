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
package net.bluemind.ui.im.client.conversation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.group.api.IGroupAsync;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.Member.Type;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.im.client.IScreen;
import net.bluemind.ui.im.client.chatroom.InviteeSearchBox;
import net.bluemind.ui.im.client.chatroom.InviteeSearchBoxOracle;
import net.bluemind.ui.im.client.chatroom.InviteeSearchSuggestion;
import net.bluemind.user.api.IUserAsync;
import net.bluemind.user.api.User;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;

public abstract class InvitationList extends PopupPanel implements IScreen {

	private static final Binder binder = GWT.create(Binder.class);

	public interface InvitationListBundle extends ClientBundle {
		@Source("InvitationList.css")
		InvitationListStyle getStyle();
	}

	public interface InvitationListStyle extends CssResource {
		public String invitationList();

		public String header();

		public String form();

		public String footer();
	}

	public static InvitationListStyle style;
	public static InvitationListBundle bundle;

	protected Map<String, Invitee> invitations;
	private final InviteeSearchBox searchBox;

	interface Binder extends UiBinder<Widget, InvitationList> {
	}

	@UiField
	protected Button submit;

	@UiField
	protected Button cancel;

	@UiField
	protected FlowPanel form;

	@UiField
	protected FlowPanel footer;

	public InvitationList() {
		setWidget(binder.createAndBindUi(this));
		bundle = GWT.create(InvitationListBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();

		setStyleName(style.invitationList());
		setAutoHideEnabled(true);

		form.setStyleName(style.form());
		footer.setStyleName(style.footer());

		invitations = new HashMap<String, Invitee>();

		SuggestOracle oracle = new InviteeSearchBoxOracle();
		searchBox = new InviteeSearchBox(oracle);
		searchBox.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {

			@Override
			public void onSelection(SelectionEvent<Suggestion> event) {
				DirEntry de = ((InviteeSearchSuggestion) event.getSelectedItem()).getDirectoryEntry();

				if (DirEntry.Kind.USER.equals(de.kind)) {
					if (!Ajax.TOKEN.getSubject().equals(de.entryUid)) {
						addToInvitees(de.email);
					}
				} else {
					addGroupToInvitees(de);
				}

				searchBox.setValue(null, false);
				searchBox.setFocus(true);
			}

		});

		form.add(searchBox);

		submit.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				submit();
			}
		});

		cancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});

	}

	private void addGroupToInvitees(DirEntry de) {
		IGroupAsync groupService = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), Ajax.TOKEN.getContainerUid());

		final IUserAsync userService = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), Ajax.TOKEN.getContainerUid());

		groupService.getMembers(de.entryUid, new AsyncHandler<List<Member>>() {

			@Override
			public void success(List<Member> value) {
				for (Member m : value) {
					if (m.type == Type.user) {

						userService.getComplete(m.uid, new AsyncHandler<ItemValue<User>>() {

							@Override
							public void success(ItemValue<User> value) {
								addToInvitees(value.value.defaultEmail().address);
							}

							@Override
							public void failure(Throwable e) {
							}
						});

					} else {
						// TODO group group ?
					}
				}

			}

			@Override
			public void failure(Throwable e) {
			}
		});

	}

	public abstract void submit();

	@Override
	public void show() {
		super.show();
		for (String k : invitations.keySet()) {
			form.remove(invitations.get(k));
		}
		invitations = new HashMap<String, Invitee>();

		Scheduler.get().scheduleDeferred(new ScheduledCommand() {

			@Override
			public void execute() {
				searchBox.setFocus(true);
			}

		});
	}

	/**
	 * @param email
	 */
	public void addToInvitees(final String email) {
		if (!invitations.containsKey(email)) {
			final Invitee invitee = new Invitee(email);

			invitee.addClickHandler(new ClickHandler() {

				@Override
				public void onClick(ClickEvent event) {
					form.remove(invitee);
					invitations.remove(email);
				}
			});

			form.insert(invitee, form.getWidgetCount() - 1);
			invitations.put(email, invitee);
		}
	}

	/**
	 * @param text
	 */
	protected void setPlaceHolder(String text) {
		searchBox.getElement().setAttribute("placeholder", text);
	}
}
