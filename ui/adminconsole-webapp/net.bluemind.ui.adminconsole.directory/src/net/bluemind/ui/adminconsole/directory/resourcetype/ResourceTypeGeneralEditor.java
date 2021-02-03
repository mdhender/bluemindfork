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
package net.bluemind.ui.adminconsole.directory.resourcetype;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.resource.api.type.gwt.js.JsResourceTypeDescriptor;
import net.bluemind.resource.api.type.gwt.js.JsResourceTypeDescriptorProperty;
import net.bluemind.resource.api.type.gwt.serder.ResourceTypeDescriptorPropertyGwtSerDer;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.editor.client.Editor;
import net.bluemind.ui.imageupload.client.ImageUpload;
import net.bluemind.ui.imageupload.client.ImageUploadHandler;

public class ResourceTypeGeneralEditor extends CompositeGwtWidgetElement {

	interface GenralUiBinder extends UiBinder<HTMLPanel, ResourceTypeGeneralEditor> {
	}

	public static final String TYPE = "bm.ac.ResourceTypeGeneralEditor";

	private static GenralUiBinder uiBinder = GWT.create(GenralUiBinder.class);

	private static final List<String> SUPPORTED_LANGUAGES = Arrays
			.asList(new String[] { "fr", "en", "de", "es", "pt", "it", "hu", "nl", "pl", "ru", "sk", "uk", "zh" });

	@UiField
	StringEdit label;

	@UiField
	CustomPropertyContainer customPropContainer;

	@UiField
	Image icon;

	private String imageUuid;

	@UiField
	ListBox templateLanguagesComboBox;

	@UiField
	Editor templateEditor;

	/** Local storage for templates. */
	private Map<String, String> templatesByLanguage = new HashMap<String, String>();

	/** Keep track of the selected template. */
	private int selectedTemplateIndex;

	protected ResourceTypeGeneralEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		icon.getElement().getStyle().setCursor(Cursor.POINTER);
		initWidget(panel);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		if (map.get("resourceType") == null) {
			GWT.log("resourceType not found..");
			return;
		}
		imageUuid = null;
		String domainUid = map.getString("domainUid");
		String s = map.getString("resourceTypeId");

		icon.setUrl("/api/resources/" + domainUid + "/type/" + s + "/icon?timestamp=" + System.currentTimeMillis());

		JsResourceTypeDescriptor rt = map.get("resourceType").cast();
		label.asEditor().setValue(rt.getLabel());
		List<Property> properties = new GwtSerDerUtils.ListSerDer<Property>(
				new ResourceTypeDescriptorPropertyGwtSerDer()).deserialize(new JSONArray(rt.getProperties().cast()));

		customPropContainer.setProperties(properties);

		this.templatesByLanguage = rt.getTemplates().asMap();

		for (final String language : SUPPORTED_LANGUAGES) {
			templateLanguagesComboBox.addItem(language);
		}

		this.templateLanguagesComboBox.setSelectedIndex(0);
		this.templateEditor.setText(this.templatesByLanguage.get(SUPPORTED_LANGUAGES.get(0)));
		this.selectedTemplateIndex = 0;
		this.templateLanguagesComboBox.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				// store the current template
				storeCurrentTemplate();
				// update the selected index
				selectedTemplateIndex = templateLanguagesComboBox.getSelectedIndex();

				// fill the editor with the new template
				templateEditor.setText(templatesByLanguage.get(SUPPORTED_LANGUAGES.get(selectedTemplateIndex)));
			}
		});
	}

	private void storeCurrentTemplate() {
		final String currentTemplate = this.templateEditor.getText();
		this.templatesByLanguage.put(SUPPORTED_LANGUAGES.get(selectedTemplateIndex), currentTemplate);
	}

	@Override
	public void saveModel(JavaScriptObject model) {

		JsMapStringJsObject map = model.cast();
		if (map.get("resourceType") == null) {
			GWT.log("resourceType not found..");
			return;
		}
		JsResourceTypeDescriptor rt = map.get("resourceType").cast();
		rt.setLabel(label.asEditor().getValue());

		JSONArray value = new GwtSerDerUtils.ListSerDer<Property>(new ResourceTypeDescriptorPropertyGwtSerDer())
				.serialize(customPropContainer.getProperties()).isArray();
		rt.setProperties(value.getJavaScriptObject().<JsArray<JsResourceTypeDescriptorProperty>>cast());

		if (imageUuid != null) {
			map.putString("resourceTypeIcon", imageUuid);
		}

		this.storeCurrentTemplate();
		rt.setTemplates(JsMapStringString.create(this.templatesByLanguage));
	}

	@UiHandler("icon")
	public void onIconClicked(ClickEvent e) {
		new ImageUpload(null, new ImageUploadHandler() {

			@Override
			public void newImage(String value) {
				imageUuid = value;
				icon.setUrl("tmpfileupload?uuid=" + imageUuid);
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

	public static void registerType() {
		GwtWidgetElement.register("bm.ac.ResourceTypeGeneralEditor",
				new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

					@Override
					public IGwtWidgetElement create(WidgetElement e) {
						return new ResourceTypeGeneralEditor();
					}
				});
	}

}
