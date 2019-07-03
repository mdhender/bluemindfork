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
package net.bluemind.ui.gwtsharing.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.api.IContainersAsync;
import net.bluemind.core.container.api.gwt.endpoint.ContainersGwtEndpoint;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.gwt.js.JsAccessControlEntry;
import net.bluemind.core.container.model.acl.gwt.serder.AccessControlEntryGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.acl.AclConstants;

public class BaseSharingsEditor extends CompositeGwtWidgetElement {

	protected final AclEdit edit;
	private final String modelId;
	private FlowPanel flowPanel;

	private ListBox containersList;
	private String selectedContainerUid;

	private Map<String, JsArray<JsAccessControlEntry>> entries;
	private Map<String, ContainerDescriptor> containers;
	private String type;
	private String ownerUid;

	public BaseSharingsEditor(String modelId, String containerType) {
		this.modelId = modelId;
		this.type = containerType;
		Map<String, String> verbs = getVerbs();

		edit = new AclEdit(verbs, AbstractDirEntryOpener.defaultOpener);
		edit.setEnable(false);

		flowPanel = new FlowPanel();
		containersList = new ListBox();
		flowPanel.add(containersList);
		flowPanel.add(edit.asWidget());

		initWidget(flowPanel);

		containersList.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				selectedContainerChanged();
			}
		});
	}

	protected void registerAclEntityValidator(IAclEntityValidator validator) {
		edit.registerValidator(validator);
	}

	protected void selectedContainerChanged() {
		String containerUid = containersList.getSelectedValue();

		if (containerUid != null && !containerUid.equals(selectedContainerUid)) {
			saveCurrent();
		}

		if (null == containers.get(containerUid)) {
			return;
		}

		selectedContainerUid = containerUid;

		edit.setContainerUid(selectedContainerUid);

		IContainersAsync containers = new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId());
		containers.get(selectedContainerUid, new DefaultAsyncHandler<ContainerDescriptor>() {

			@Override
			public void success(ContainerDescriptor value) {
				Map<String, String> verbs = new HashMap<>();
				AclConstants constants = GWT.create(AclConstants.class);

				if (type.equals("calendar")) {
					boolean isDomainCalendar = value.ownerDirEntryPath.startsWith(value.domainUid + "/calendars/");
					if (!value.readOnly && !isDomainCalendar) {
						verbs.put("access", constants.aclCalendarAccess());
					}
					verbs.put("read",
							isDomainCalendar ? constants.aclDomainCalendarRead() : constants.aclCalendarReadOnly());
					if (!value.readOnly) {
						verbs.put("write",
								isDomainCalendar ? constants.aclDomainCalendarWrite() : constants.aclCalendarWrite());
						verbs.put("admin",
								isDomainCalendar ? constants.aclDomainCalendarAdmin() : constants.aclCalendarAdmin());
					}
				} else if ("freebusy".equals(type)) {
					verbs.put("read", constants.aclFreebusyRead());
					if (!value.readOnly) {
						verbs.put("admin", constants.aclFreebusyAdmin());
					}
				} else if ("addressbook".equals(type)) {
					verbs.put("read", constants.aclBookRead());
					if (!value.readOnly) {
						verbs.put("write", constants.aclBookWrite());
						verbs.put("admin", constants.aclBookAdmin());
					}
				} else if ("mailboxacl".equals(type)) {
					verbs.put("send-on-behalf", constants.aclMailSendOnBehalf());
					verbs.put("read", constants.aclMailRead());
					if (!value.readOnly) {
						verbs.put("write", constants.aclMailWrite());
						verbs.put("admin", constants.aclMailAdmin());
					}
				} else {
					verbs.put("read", constants.aclRead());
					if (!value.readOnly) {
						verbs.put("write", constants.aclWrite());
						verbs.put("admin", constants.aclAdmin());
					}
				}

				edit.setVerbs(verbs);

				JsArray<JsAccessControlEntry> acl = entries.get(selectedContainerUid);
				if (acl == null) {
					edit.setEnable(true);
					edit.setValue(Arrays.<AccessControlEntry>asList());
					if ("calendar".equals(type)) {
						edit.setAddressesSharing(type);
					}
				} else {
					edit.setEnable(true);
					List<AccessControlEntry> aclModel = new GwtSerDerUtils.ListSerDer<>(
							new AccessControlEntryGwtSerDer()).deserialize(new JSONArray(acl));
					if ("calendar".equals(type)) {
						edit.setAddressesSharing(type);
					}
					edit.setValue(aclModel);
				}
			}

		});

	}

	private void saveCurrent() {
		JsArray<JsAccessControlEntry> aclModel = new GwtSerDerUtils.ListSerDer<>(new AccessControlEntryGwtSerDer())
				.serialize(edit.getValue()).isArray().getJavaScriptObject().cast();
		entries.put(selectedContainerUid, aclModel);
		GWT.log("save for " + selectedContainerUid);
	}

	protected Map<String, String> getVerbs() {
		return computeVerbs();
	}

	protected Map<String, String> computeVerbs() {
		Map<String, String> verbs = new HashMap<>();
		AclConstants constants = GWT.create(AclConstants.class);

		if (type.equals("calendar")) {
			verbs.put("access", constants.aclCalendarAccess());
			verbs.put("read", constants.aclCalendarReadOnly());
			verbs.put("write", constants.aclCalendarWrite());
			verbs.put("admin", constants.aclCalendarAdmin());
		} else if ("freebusy".equals(type)) {
			verbs.put("read", constants.aclFreebusyRead());
			verbs.put("admin", constants.aclFreebusyAdmin());
		} else if ("addressbook".equals(type)) {
			verbs.put("read", constants.aclBookRead());
			verbs.put("write", constants.aclBookWrite());
			verbs.put("admin", constants.aclBookAdmin());
		} else if ("mailboxacl".equals(type)) {
			verbs.put("send-on-behalf", constants.aclMailSendOnBehalf());
			verbs.put("read", constants.aclMailRead());
			verbs.put("write", constants.aclMailWrite());
			verbs.put("admin", constants.aclMailAdmin());
		} else {
			verbs.put("read", constants.aclRead());
			verbs.put("write", constants.aclWrite());
			verbs.put("admin", constants.aclAdmin());
		}

		return verbs;
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		saveCurrent();
		final SharingsModel sharingsModel = SharingsModel.get(model, modelId);

		for (ContainerDescriptor desc : containers.values()) {
			sharingsModel.setJsAcl(desc.uid, entries.get(desc.uid));
		}
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		entries = new HashMap<>();
		containers = new HashMap<>();
		containersList.clear();

		final SharingsModel sharingsModel = SharingsModel.get(model, modelId);
		JsMapStringJsObject map = model.cast();

		ownerUid = map.getString("userId");
		edit.setDomainUid(map.getString("domainUid"));

		ContainersGwtEndpoint e = new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId());
		e.getContainers(Arrays.asList(sharingsModel.getContainers()),
				new DefaultAsyncHandler<List<ContainerDescriptor>>() {

					@Override
					public void success(List<ContainerDescriptor> value) {

						int i = 0;
						for (ContainerDescriptor cd : value) {
							JsArray<JsAccessControlEntry> acl = sharingsModel.getJsAcl(cd.uid);

							entries.put(cd.uid, acl);
							containersList.addItem(cd.name, cd.uid);
							containers.put(cd.uid, cd);
							if (cd.defaultContainer && cd.owner.equals(ownerUid)) {
								containersList.setSelectedIndex(i);
							}

							i++;
						}

						selectedContainerChanged();

					}
				});

	}

}
