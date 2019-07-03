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

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;

import net.bluemind.core.api.gwt.js.JsEmail;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.group.api.gwt.js.JsGroup;
import net.bluemind.group.api.gwt.js.JsMember;
import net.bluemind.group.api.gwt.js.JsMemberType;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.adminconsole.base.ui.UserOrExternalUserOrGroupEntityEdit;
import net.bluemind.ui.adminconsole.directory.group.l10n.GroupConstants;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.common.client.forms.finder.ServerFinder;

public class NewGroup extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.QCreateGroupWidget";

	private static NewGroupUiBinder uiBinder = GWT.create(NewGroupUiBinder.class);

	interface NewGroupUiBinder extends UiBinder<HTMLPanel, NewGroup> {

	}

	private ItemValue<Domain> domain;

	private HTMLPanel dlp;

	@UiField
	DelegationEdit delegation;

	@UiField
	StringEdit name;

	@UiField
	UserOrExternalUserOrGroupEntityEdit ugEdit;

	@UiField
	TextArea desc;

	@UiField
	CheckBox hidden;

	@UiField
	CheckBox hideMembers;

	@UiField
	Label errorLabel;

	@UiField
	ListBox mailBackend;

	@UiField
	HTMLPanel mailBackendPanel;

	private ItemValue<Domain> groupDomain;

	ServerFinder serverFinder = new ServerFinder("mail/imap");

	private NewGroup() {
		dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);
		name.setId("new-group-name");
		desc.getElement().setId("new-group-id");
		hidden.getElement().setId("new-group-hidden");
		hideMembers.getElement().setId("new-group-hide-members");
		// needed to embed a docklayoutpanel
		dlp.setHeight("100%");
		this.groupDomain = DomainsHolder.get().getSelectedDomain();
		updateDomainChange(groupDomain);
	}

	private void updateDomainChange(ItemValue<Domain> d) {
		this.domain = d;
		ugEdit.setDomain(d);
		delegation.setDomain(d.uid);
		if (domain.value.global) {
			errorLabel.setText(ErrorCodeTexts.INST.getString("NOT_IN_GLOBAL_DOMAIN"));
		} else {
			errorLabel.setText("");
		}
		serverFinder.find(this.domain.uid, mailBackend, mailBackendPanel);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		if (map.get("domain") != null) {
			JsItemValue<JsDomain> domain = map.get("domain").cast();

			ItemValue<Domain> d = new ItemValueGwtSerDer<>(new DomainGwtSerDer()).deserialize(new JSONObject(domain));
			updateDomainChange(d);
		}
		JsGroup group = map.get("group").cast();

		hidden.setValue(group.getArchived());
		hideMembers.setValue(group.getHiddenMembers());
		delegation.asEditor().setValue(group.getOrgUnitUid());
		name.asEditor().setValue(group.getName());
		desc.asEditor().setValue(group.getDescription());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JsGroup group = map.get("group").cast();
		JsArray<JsMember> jsMembers = map.get("members").cast();

		group.setHidden(hidden.getValue());
		group.setHiddenMembers(hideMembers.getValue());

		group.setOrgUnitUid(delegation.asEditor().getValue());
		group.setName(name.asEditor().getValue());
		group.setDescription(desc.asEditor().getValue());
		group.setEmails(JsArray.createArray().<JsArray<JsEmail>>cast());
		group.setMailArchived(false);
		group.setDataLocation(mailBackend.getSelectedValue());

		Set<DirEntry> members = ugEdit.getValues();
		GWT.log("should create with " + members.size() + " members");

		for (DirEntry member : members) {
			JsMember gMember = JsMember.create();
			gMember.setUid(member.entryUid);

			switch (member.kind) {
			case USER:
				gMember.setType(JsMemberType.user());
				break;
			case EXTERNALUSER:
				gMember.setType(JsMemberType.external_user());
				break;
			case GROUP:
				gMember.setType(JsMemberType.group());
				break;
			default:
				GWT.log("Unknown type of member (UID: " + member.entryUid + ")");
				break;
			}

			jsMembers.push(gMember);
		}
	}

	@UiFactory
	GroupConstants getConstants() {
		return GroupConstants.INST;
	}

	public static void registerType() {
		GwtWidgetElement.register("bm.ac.QCreateGroupWidget",
				new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

					@Override
					public IGwtWidgetElement create(WidgetElement e) {
						return new NewGroup();
					}
				});
		GWT.log("bm.ac.QCreateGroupWidget registred");
	}
}
