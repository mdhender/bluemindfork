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
package net.bluemind.ui.mailbox.identity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.gwt.endpoint.MailboxIdentityGwtEndpoint;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.tag.UUID;
import net.bluemind.ui.common.client.icon.Trash;
import net.bluemind.user.api.UserMailIdentity;
import net.bluemind.user.api.gwt.js.JsUserMailIdentity;
import net.bluemind.user.api.gwt.serder.UserMailIdentityGwtSerDer;

public class IdentityManagement extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.mailbox.IdentitiesEditor";

	public static interface Resources extends ClientBundle {
		@Source("IdentityEdit.css")
		Style editStyle();
	}

	public static interface Style extends CssResource {

		String identities();

		String headers();

		String name();

		String label();

		String value();

		String action();

		String identity();

		String current();

		String actionCell();

	}

	interface IdentityManagementUiBinder extends UiBinder<HTMLPanel, IdentityManagement> {

	}

	private static IdentityManagementUiBinder binder = GWT.create(IdentityManagementUiBinder.class);

	@UiField
	FlexTable table;

	@UiField
	Label noIdentity;

	@UiField
	Button add;

	protected List<IdentityDescription> identities;

	private List<IdentityDescription> templates;

	protected Map<String, UserMailIdentity> toUpdate = new HashMap<>();
	protected Map<String, UserMailIdentity> toCreate = new HashMap<>();
	protected Set<String> toDelete = new TreeSet<>();

	private static final Resources res = GWT.create(Resources.class);

	private final Style s;

	protected String mboxUid;

	protected String domainUid;

	protected String defaultIdentity;

	protected boolean supportsExternalIdentities;

	private String mboxName;

	private boolean noDefaultIdentity;

	/**
	 * @param token
	 * @param user
	 */
	public IdentityManagement() {
		s = res.editStyle();
		loadUI();

	}

	/**
	 * @param at
	 * @param entity
	 */
	private void loadUI() {
		initWidget(binder.createAndBindUi(this));

		identities = new ArrayList<IdentityDescription>();
		templates = new ArrayList<IdentityDescription>();

		toDelete.clear();
		toUpdate.clear();
		toCreate.clear();
		s.ensureInjected();

		table.setStyleName(s.identities());

		add.getElement().setId("identity-management-add");
		add.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				final IdentityEditDialog ied = new IdentityEditDialog(templates, mboxUid, supportsExternalIdentities);
				ied.setAction(new ScheduledCommand() {

					@Override
					public void execute() {
						UserMailIdentity id = ied.getIdentity();
						String uid = UUID.uuid();
						toCreate.put(uid, id);

						IdentityDescription idd = new IdentityDescription();
						idd.email = id.email;
						idd.mbox = id.mailboxUid;
						idd.name = id.name;
						idd.displayname = id.displayname;
						idd.id = uid;
						idd.signature = id.signature;
						identities.add(idd);
						draw();
					}

				});
				SizeHint sh = ied.getSizeHint();

				final DialogBox os = new DialogBox();
				os.addStyleName("dialog");
				ied.setSize(sh.getWidth() + "px", sh.getHeight() + "px");
				ied.setOverlay(os);
				os.setWidget(ied);
				os.setGlassEnabled(true);
				os.setAutoHideEnabled(false);
				os.setGlassStyleName("modalOverlay");
				os.setModal(false);
				os.center();
				os.show();

			}
		});

	}

	/**
	 * @param i
	 */
	private void setDefault(IdentityDescription i) {
		defaultIdentity = i.id;
	}

	@UiFactory
	IdentityConstants getTexts() {
		return IdentityConstants.INST;
	}

	private void addGridRow(final IdentityDescription identityDescription, final int pos) {

		int row = table.getRowCount();

		Trash trash = new Trash();
		trash.setId("identity-management-trash-"); // FIXME+ i.getId());
		trash.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (Window.confirm(IdentityConstants.INST.confirmDelete(identityDescription.name))) {

					IdentityDescription desc = identities.remove(pos);

					toUpdate.remove(desc.id);
					if (!toCreate.containsKey(desc.id)) {
						toDelete.add(desc.id);
					} else {
						toCreate.remove(desc.id);
					}
					draw();
				}
			}
		});

		RadioButton def = new RadioButton("isdefault");

		def.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				setDefault(identityDescription);
			}
		});

		Anchor edit = new Anchor(IdentityConstants.INST.update());
		edit.getElement().setId("identity-edit-" + identityDescription.id);
		edit.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				editIdentity(pos);

			}
		});

		if (defaultIdentity != null && identityDescription.id.equals(defaultIdentity)) {
			trash.setVisible(false);
			def.setValue(true);
		}

		String name = identityDescription.name != null && !identityDescription.name.isEmpty() ? identityDescription.name
				: getTexts().noName();
		String displayName = identityDescription.displayname != null && !identityDescription.displayname.isEmpty()
				? identityDescription.displayname
				: "";

		int index = 0;
		if (!this.noDefaultIdentity) {
			table.setWidget(row, index++, def);
		}
		table.setText(row, index++, name);
		table.setText(row, index++, identityDescription.email);
		table.setText(row, index++, displayName);
		table.setWidget(row, index++, edit);
		table.setWidget(row, index++, trash);

		table.getCellFormatter().setStyleName(row, 0, s.actionCell());
	}

	protected void editIdentity(final int pos) {
		final IdentityDescription id = identities.get(pos);
		UserMailIdentity identity = null;
		if (toCreate.containsKey(id.id)) {
			identity = toCreate.get(id.id);
		} else if (toUpdate.containsKey(id.id)) {
			identity = toUpdate.get(id.id);
		}

		if (identity == null) {
			loadIdentity(id, new DefaultAsyncHandler<UserMailIdentity>() {

				@Override
				public void success(UserMailIdentity value) {
					toUpdate.put(id.id, value);
					editIdentity(pos);
				}
			});
			return;
		}

		final IdentityEditDialog ied = new IdentityEditDialog(identity, templates, mboxUid, supportsExternalIdentities);
		ied.setAction(new ScheduledCommand() {

			@Override
			public void execute() {

				UserMailIdentity identity = ied.getIdentity();

				if (toCreate.containsKey(id.id)) {
					toCreate.put(id.id, identity);
				} else {
					toUpdate.put(id.id, identity);
				}

				IdentityDescription idd = new IdentityDescription();
				idd.email = identity.email;
				idd.mbox = mboxUid;
				idd.name = identity.name;
				idd.id = identities.get(pos).id;
				idd.displayname = identity.displayname;
				idd.signature = identity.signature;
				idd.mboxName = mboxName;
				identities.set(pos, idd);
				draw();
			}

		});

		SizeHint sh = ied.getSizeHint();
		final DialogBox os = new DialogBox();

		os.addStyleName("dialog");
		ied.setSize(sh.getWidth() + "px", sh.getHeight() + "px");
		ied.setOverlay(os);
		os.setWidget(ied);
		os.setGlassEnabled(true);
		os.setAutoHideEnabled(false);
		os.setGlassStyleName("modalOverlay");
		os.setModal(false);
		os.center();
		os.show();

	}

	protected void loadIdentity(final IdentityDescription id, final AsyncHandler<UserMailIdentity> handler) {
		MailboxIdentityGwtEndpoint mie = new MailboxIdentityGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid, mboxUid);
		mie.get(id.id, new DefaultAsyncHandler<Identity>() {

			@Override
			public void success(Identity value) {
				UserMailIdentity id = new UserMailIdentity();
				id.displayname = value.displayname;
				id.email = value.email;
				id.format = value.format;
				id.isDefault = value.isDefault;
				id.name = value.name;
				id.sentFolder = value.sentFolder;
				id.signature = value.signature;
				id.mailboxUid = mboxUid;
				handler.success(id);
			}

			@Override
			public void failure(Throwable e) {
				handler.failure(e);
			}
		});

	}

	protected void draw() {
		table.removeAllRows();
		table.getRowFormatter().setStyleName(0, s.headers());
		int index = 0;
		if (!this.noDefaultIdentity) {
			table.setWidget(0, index++, new Label(getTexts().defaultIdentity()));
		}
		table.setWidget(0, index++, new Label(getTexts().name()));
		table.setWidget(0, index++, new Label(getTexts().email()));
		table.setWidget(0, index++, new Label(getTexts().displayname()));
		table.setWidget(0, index++, new Label(""));
		table.setWidget(0, index++, new Label(""));
		table.getCellFormatter().setStyleName(0, 0, s.name());
		table.getCellFormatter().setStyleName(0, 1, s.name());
		table.getCellFormatter().setStyleName(0, 2, s.name());
		table.getCellFormatter().setStyleName(0, 3, s.name());
		table.getCellFormatter().setStyleName(0, 5, s.action());
		hasIdentities(!identities.isEmpty());
		int pos = 0;
		for (IdentityDescription i : identities) {
			addGridRow(i, pos);
			pos++;
		}
	}

	private void hasIdentities(boolean has) {
		table.setVisible(has);
		noIdentity.setVisible(!has);
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		MailIdentitiesModel mim = model.cast();

		JsArray<JsItemValue<JsUserMailIdentity>> update = JsArray.createArray().cast();

		for (Map.Entry<String, UserMailIdentity> u : toUpdate.entrySet()) {
			ItemValue<UserMailIdentity> iv = new ItemValue<>();
			iv.uid = u.getKey();
			iv.value = u.getValue();

			update.push(new ItemValueGwtSerDer<>(new UserMailIdentityGwtSerDer()).serialize(iv).isObject()
					.getJavaScriptObject().<JsItemValue<JsUserMailIdentity>>cast());
		}
		mim.setUpdate(update);

		JsArray<JsItemValue<JsUserMailIdentity>> create = JsArray.createArray().cast();

		for (Map.Entry<String, UserMailIdentity> u : toCreate.entrySet()) {
			ItemValue<UserMailIdentity> iv = new ItemValue<>();
			iv.uid = u.getKey();
			iv.value = u.getValue();

			create.push(new ItemValueGwtSerDer<>(new UserMailIdentityGwtSerDer()).serialize(iv).isObject()
					.getJavaScriptObject().<JsItemValue<JsUserMailIdentity>>cast());
		}
		mim.setCreate(create);

		JsArrayString del = JsArrayString.createArray().cast();
		for (String id : toDelete) {
			del.push(id);
		}

		mim.setDelete(del);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		MailIdentitiesModel mim = model.cast();

		this.noDefaultIdentity = mim.isNoDefaultIdentity();

		this.supportsExternalIdentities = mim.isSupportsExternalIdentities();
		defaultIdentity = null;

		toDelete.clear();
		toUpdate.clear();
		toCreate.clear();

		identities.clear();
		identities.addAll(mim.getIdentitiesAsList());

		templates.clear();

		// use a Set and a custom decorator (with equals & hashcode) to avoid
		// duplicates and fill with mailbox and user identities
		final Set<IdDescDeco> idDescDecos = new HashSet<IdDescDeco>();
		idDescDecos.addAll(IdDescDeco.fromIdentityDescriptions(mim.getIdentitiesTemplatesAsList()));
		idDescDecos.addAll(IdDescDeco.fromIdentityDescriptions(this.identities));
		templates.addAll(IdDescDeco.toIdentityDescriptions(idDescDecos));

		domainUid = mim.getDomainUid();
		mboxUid = mim.getMailboxUid();
		mboxName = mim.getMailboxName();
		for (IdentityDescription id : identities) {
			if (id.isDefault != null && id.isDefault == true) {
				defaultIdentity = id.id;
				break;
			}
		}
		draw();
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new IdentityManagement();
			}
		});

	}
}
