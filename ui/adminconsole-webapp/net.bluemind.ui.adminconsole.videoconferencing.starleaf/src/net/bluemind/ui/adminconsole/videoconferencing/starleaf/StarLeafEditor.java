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
package net.bluemind.ui.adminconsole.videoconferencing.starleaf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
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
import net.bluemind.ui.adminconsole.videoconferencing.starleaf.l10n.StarLeafConstants;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.videoconferencing.api.IVideoConferenceUids;
import net.bluemind.videoconferencing.api.IVideoConferencingPromise;
import net.bluemind.videoconferencing.api.VideoConferencingResourceDescriptor;
import net.bluemind.videoconferencing.api.gwt.endpoint.VideoConferencingGwtEndpoint;

public class StarLeafEditor extends CompositeGwtWidgetElement {

	static final String TYPE = "bm.ac.StarLeafEditor";

	private static final String PROVIDER_NAME = "StarLeaf";
	private static final String PROVIDER_TYPE = "videoconferencing-starleaf";

	private static final String SETTINGS_TOKEN = "token";

	private static StarLeafUiBinder uiBinder = GWT.create(StarLeafUiBinder.class);

	interface StarLeafUiBinder extends UiBinder<HTMLPanel, StarLeafEditor> {
	}

	@UiField
	TextBox token;

	@UiField
	Button deleteBtn;

	@UiHandler("deleteBtn")
	void deleteClick(ClickEvent e) {
		if (Window.confirm(StarLeafConstants.INST.deleteBtnConfirm())) {
			removeResource();
		}
	}

	private String domainUid;

	private String resourceUid;

	protected StarLeafEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new StarLeafEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);

		final JsMapStringJsObject map = model.cast();
		domainUid = map.getString("domainUid");

		IResourcesPromise resourceService = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();

		resourceService.byType(IVideoConferenceUids.RESOURCETYPE_UID).thenAccept(uids -> {
			if (uids != null && !uids.isEmpty()) {
				uids.forEach(uid -> {
					resourceService.get(uid).thenAccept(res -> {
						boolean found = false;
						for (int i = 0; i < res.properties.size(); i++) {
							PropertyValue prop = res.properties.get(i);
							if (IVideoConferenceUids.PROVIDER_TYPE.equals(prop.propertyId)
									&& PROVIDER_TYPE.equals(prop.value)) {
								found = true;
							}
						}

						if (found) {
							resourceUid = uid;
							IContainerManagementPromise containerMgmt = new ContainerManagementGwtEndpoint(
									Ajax.TOKEN.getSessionId(), getResourceSettingsContainer(resourceUid)).promiseApi();
							containerMgmt.getSettings().thenAccept(settings -> {
								String slToken = settings.get(SETTINGS_TOKEN);
								if (slToken != null) {
									token.setValue(slToken);
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

		String slToken = token.asEditor().getValue();
		if (slToken != null) {
			if (resourceUid != null) {
				setResourceSettings(resourceUid);
			} else {
				final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
				createResource(uid).thenAccept(v -> {
					setResourceSettings(uid);
				});
			}
		} else {
			// TOOD warning before deletion?
			removeResource();
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

		String slToken = token.asEditor().getValue();
		settings.put(SETTINGS_TOKEN, slToken);

		containerMgmt.setSettings(settings);
	}

	private void removeResource() {
		IResourcesPromise resourceService = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		resourceService.delete(resourceUid).thenAccept(res -> {
			token.asEditor().setValue(null);
		});
	}

	private String getResourceSettingsContainer(String resourceUid) {
		return resourceUid + "-settings-container";
	}
}
