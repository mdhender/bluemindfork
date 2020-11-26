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
package net.bluemind.ui.adminconsole.directory.resource;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.resource.api.gwt.endpoint.ResourcesGwtEndpoint;
import net.bluemind.resource.api.gwt.js.JsResourceDescriptor;
import net.bluemind.resource.api.gwt.js.JsResourceDescriptorPropertyValue;
import net.bluemind.resource.api.gwt.js.JsResourceReservationMode;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.resource.api.type.gwt.endpoint.ResourceTypesGwtEndpoint;
import net.bluemind.ui.admin.client.forms.TextEdit;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.adminconsole.base.ui.MailAddressTableEditor;
import net.bluemind.ui.adminconsole.directory.resource.l10n.ResourceConstants;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.imageupload.client.ImageUpload;
import net.bluemind.ui.imageupload.client.ImageUploadHandler;

public class EditResource extends CompositeGwtWidgetElement {

	interface EditResourceUiBinder extends UiBinder<HTMLPanel, EditResource> {

	}

	public static final String TYPE = "bm.ac.ResourceEditor";

	private static EditResourceUiBinder binder = GWT.create(EditResourceUiBinder.class);

	@UiField
	StringEdit name;

	@UiField
	DelegationEdit delegation;

	@UiField
	TextEdit description;

	@UiField
	MailAddressTableEditor mailTable;

	@UiField
	Label type;

	@UiField
	RadioButton reservationModeOwner;

	@UiField
	RadioButton reservationModeAutoAccept;

	@UiField
	CheckBox reservationModeAutoRefuse;

	@UiField
	HTMLPanel customPropertiesContainer;

	@UiField
	Label customPropTitle;

	@UiField
	Image icon;

	private ResourceProperties resourceProperties;
	private String iconUuid;
	private JsResourceDescriptor resourceDescriptor;
	private String domainUid;

	private String locale;

	public EditResource(WidgetElement widgetModel) {
		locale = LocaleInfo.getCurrentLocale().getLocaleName();
		if (locale.length() > 2) {
			locale = locale.substring(0, 2);
		}
		HTMLPanel dlp = binder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		customPropTitle.setVisible(false);
		name.setId("edit-resource-name");

		icon.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				ImageUpload.show(null, new ImageUploadHandler() {

					@Override
					public void newImage(String value) {
						iconUuid = value;
						icon.setUrl("tmpfileupload?uuid=" + value);

					}

					@Override
					public void failure(Throwable exception) {
						// TODO Auto-generated method stub

					}

					@Override
					public void deleteCurrent() {
						// TODO Auto-generated method stub

					}

					@Override
					public void cancel() {
					}
				});

			}
		});

		name.setReadOnly(widgetModel.isReadOnly());
		delegation.setReadOnly(widgetModel.isReadOnly());
		description.setReadOnly(widgetModel.isReadOnly());
		mailTable.asWidget().setReadOnly(widgetModel.isReadOnly());
		reservationModeOwner.addClickHandler(evt -> validateReservationMode());
		reservationModeAutoAccept.addClickHandler(evt -> validateReservationMode());
	}

	protected void loadResourceProperties(String resourceTypeIdentifier) {
		ResourceTypesGwtEndpoint ResourceTypes = new ResourceTypesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		ResourceTypes.get(resourceTypeIdentifier, new AsyncHandler<ResourceTypeDescriptor>() {
			@Override
			public void success(ResourceTypeDescriptor value) {
				loadType(value);
				loadProperties(value);
			}

			@Override
			public void failure(Throwable e) {
				GWT.log("error during type loading " + e);
			}
		});
	}

	private void loadType(ResourceTypeDescriptor value) {
		type.setText(value.label);
	}

	private void loadProperties(ResourceTypeDescriptor descriptor) {
		List<Property> properties = descriptor.properties;
		resourceProperties.load(properties);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		final JsMapStringJsObject map = model.cast();
		String rUid = map.getString("resourceId");
		this.domainUid = map.getString("domainUid");
		resourceDescriptor = map.get("resource").cast();

		resourceProperties = new ResourceProperties(locale, customPropertiesContainer, customPropTitle);
		resourceProperties.loadProps(resourceDescriptor.getProperties());
		name.asEditor().setValue(resourceDescriptor.getLabel());
		description.asEditor().setValue(resourceDescriptor.getDescription());
		loadResourceProperties(resourceDescriptor.getTypeIdentifier());
		loadIcon(rUid);

		if (map.get("domain") != null) {
			mailTable.setDomain(
					new ItemValueGwtSerDer<>(new DomainGwtSerDer()).deserialize(new JSONObject(map.get("domain"))));
		}
		mailTable.asEditor().setValue(resourceDescriptor.getEmails());
		mailTable.asWidget().setDefaultLogin(name.getStringValue());

		if (resourceDescriptor.getReservationMode().toString()
				.equals(JsResourceReservationMode.OWNER_MANAGED().toString())) {
			reservationModeOwner.setValue(true);
		} else {
			delegation.setDomain(domainUid);
			reservationModeAutoAccept.setValue(true);
			reservationModeAutoRefuse.setValue(resourceDescriptor.getReservationMode().toString()
					.equals(JsResourceReservationMode.AUTO_ACCEPT_REFUSE().toString()));
		}
		validateReservationMode();
		delegation.setDomain(domainUid);
		delegation.asEditor().setValue(resourceDescriptor.getOrgUnitUid());
	}

	private void validateReservationMode() {
		if (reservationModeOwner.getValue()) {
			reservationModeAutoRefuse.setEnabled(false);
			reservationModeAutoRefuse.setValue(false);
			reservationModeAutoAccept.setValue(false);
		} else {
			reservationModeAutoRefuse.setEnabled(true);
		}
	}

	native String atob(String encoded)
	/*-{
		return atob(encoded);
	}-*/;

	private void loadIcon(String rUid) {

		new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).getIcon(rUid, new DefaultAsyncHandler<byte[]>() {

			@Override
			public void success(byte[] value) {
				String b64 = atob(new String(value));
				icon.getElement().setAttribute("src", "data:image/png;base64," + b64);
			}

		});

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		final JsMapStringJsObject map = model.cast();
		resourceDescriptor = map.get("resource").cast();

		resourceDescriptor.setLabel(name.asEditor().getValue());
		resourceDescriptor.setDescription(description.asEditor().getValue());
		resourceDescriptor.setEmails(mailTable.asEditor().getValue());
		JsArray<JsResourceDescriptorPropertyValue> values = JsArray.createArray().cast();
		resourceProperties.saveValues(values);
		resourceDescriptor.setProperties(values);

		if (reservationModeOwner.getValue()) {
			resourceDescriptor.setReservationMode(JsResourceReservationMode.OWNER_MANAGED());
		} else {
			if (reservationModeAutoRefuse.getValue()) {
				resourceDescriptor.setReservationMode(JsResourceReservationMode.AUTO_ACCEPT_REFUSE());
			} else {
				resourceDescriptor.setReservationMode(JsResourceReservationMode.AUTO_ACCEPT());
			}
		}

		if (iconUuid != null) {
			map.putString("resourceIcon", iconUuid);
		}

		resourceDescriptor.setOrgUnitUid(delegation.asEditor().getValue());
	}

	@UiFactory
	ResourceConstants getConstants() {
		return ResourceConstants.INST;
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditResource(e);
			}
		});
	}

}
