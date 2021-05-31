/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.videoconferencing.bluemind;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.container.api.IContainerManagementPromise;
import net.bluemind.core.container.api.gwt.endpoint.ContainerManagementGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.resource.api.IResourcesPromise;
import net.bluemind.resource.api.ResourceDescriptor.PropertyValue;
import net.bluemind.resource.api.gwt.endpoint.ResourcesGwtEndpoint;
import net.bluemind.ui.adminconsole.videoconferencing.bluemind.l10n.BlueMindEditorConstants;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.editor.client.Editor;
import net.bluemind.videoconferencing.api.IVideoConferenceUids;
import net.bluemind.videoconferencing.api.IVideoConferencingPromise;
import net.bluemind.videoconferencing.api.VideoConferencingResourceDescriptor;
import net.bluemind.videoconferencing.api.gwt.endpoint.VideoConferencingGwtEndpoint;

public class BlueMindEditor extends CompositeGwtWidgetElement {
	static final String TYPE = "bm.ac.BlueMindEditor";

	// FIXME name/type from BlueMindProvider
	private static final String PROVIDER_NAME = "BlueMind";
	private static final String PROVIDER_TYPE = "videoconferencing-bluemind";

	private static final String SETTINGS_TEMPLATES = "templates";

	private static final List<String> SUPPORTED_LANGUAGES = Arrays
			.asList(new String[] { "fr", "en", "de", "es", "pt", "it", "hu", "nl", "pl", "ru", "sk", "uk", "zh" });

	private static BlueMindEditorUiBinder uiBinder = GWT.create(BlueMindEditorUiBinder.class);

	interface BlueMindEditorUiBinder extends UiBinder<HTMLPanel, BlueMindEditor> {
	}

	@UiField
	Editor templateEditor;

	@UiField
	ListBox templateLanguagesComboBox;

	@UiField
	Button deleteBtn;

	@UiHandler("deleteBtn")
	void deleteClick(ClickEvent e) {
		if (Window.confirm(BlueMindEditorConstants.INST.deleteBtnConfirm())) {
			removeResource();
		}
	}

	private String domainUid;

	private String resourceUid;

	/** Local storage for templates. */
	private Map<String, String> templatesByLanguage = new HashMap<>();

	/** Keep track of the selected template. */
	private int selectedTemplateIndex;

	protected BlueMindEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new BlueMindEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);
		deleteBtn.setVisible(false);

		final JsMapStringJsObject map = model.cast();
		domainUid = map.getString("domainUid");

		for (String language : SUPPORTED_LANGUAGES) {
			templateLanguagesComboBox.addItem(language);
		}

		templateLanguagesComboBox.setSelectedIndex(0);
		selectedTemplateIndex = 0;
		templateLanguagesComboBox.addChangeHandler(new ChangeHandler() {

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

		IResourcesPromise resourceService = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();

		resourceService.byType(IVideoConferenceUids.RESOURCETYPE_UID).thenAccept(uids -> {
			if (uids != null && !uids.isEmpty()) {
				uids.forEach(uid -> {
					resourceService.get(uid).thenAccept(res -> {
						boolean isBmVideo = false;
						for (int i = 0; i < res.properties.size(); i++) {
							PropertyValue prop = res.properties.get(i);
							if (IVideoConferenceUids.PROVIDER_TYPE.equals(prop.propertyId)
									&& PROVIDER_TYPE.equals(prop.value)) {
								isBmVideo = true;
							}
						}
						if (isBmVideo) {
							deleteBtn.setVisible(true);

							resourceUid = uid;
							IContainerManagementPromise containerMgmt = new ContainerManagementGwtEndpoint(
									Ajax.TOKEN.getSessionId(), getResourceSettingsContainer(resourceUid)).promiseApi();
							containerMgmt.getSettings().thenAccept(settings -> {
								String templates = settings.get(SETTINGS_TEMPLATES);
								if (templates != null) {
									JavaScriptObject safeEval = JsonUtils.safeEval(templates);
									JsMapStringString aa = safeEval.cast();
									templatesByLanguage = aa.asMap();
									templateEditor.setText(templatesByLanguage.get(SUPPORTED_LANGUAGES.get(0)));
								}

							});
						}
					});
				});
			}
		});

	}

	@Override
	public void saveModel(JavaScriptObject model) {

		if (resourceUid != null) {
			setResourceSettings(resourceUid);
		} else {
			storeCurrentTemplate();
			if (!templatesByLanguage.isEmpty()) {
				final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
				createResource(uid).thenAccept(v -> {
					setResourceSettings(uid);
				});
			}
		}

	}

	private CompletableFuture<Void> createResource(String uid) {
		IVideoConferencingPromise videoConfService = new VideoConferencingGwtEndpoint(Ajax.TOKEN.getSessionId(),
				domainUid).promiseApi();
		return videoConfService.createResource(uid,
				VideoConferencingResourceDescriptor.create(PROVIDER_NAME, PROVIDER_TYPE, Collections.emptyList()));
	}

	private void setResourceSettings(String resourceUid) {
		IContainerManagementPromise containerMgmt = new ContainerManagementGwtEndpoint(Ajax.TOKEN.getSessionId(),
				getResourceSettingsContainer(resourceUid)).promiseApi();

		Map<String, String> settings = new HashMap<>();

		storeCurrentTemplate();
		String templates = JsonUtils.stringify(JsMapStringString.create(templatesByLanguage));
		settings.put(SETTINGS_TEMPLATES, templates);

		containerMgmt.setSettings(settings);
	}

	private void removeResource() {
		IResourcesPromise resourceService = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		resourceService.delete(resourceUid).thenAccept(v -> {

			// reset form
			templateLanguagesComboBox.setSelectedIndex(0);
			selectedTemplateIndex = 0;
			resourceUid = null;
			templatesByLanguage = new HashMap<>();
			templateEditor.setText(null);
		});
	}

	private void storeCurrentTemplate() {
		String currentTemplate = templateEditor.getText();
		if (currentTemplate != null && !currentTemplate.trim().equals("")) {
			templatesByLanguage.put(SUPPORTED_LANGUAGES.get(selectedTemplateIndex), currentTemplate);
		}
	}

	private String getResourceSettingsContainer(String resourceUid) {
		return resourceUid + "-settings-container";
	}

}
