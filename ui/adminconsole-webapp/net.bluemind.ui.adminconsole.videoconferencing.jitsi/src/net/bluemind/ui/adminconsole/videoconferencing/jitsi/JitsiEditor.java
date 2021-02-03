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
package net.bluemind.ui.adminconsole.videoconferencing.jitsi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.CalendarSettingsData.Day;
import net.bluemind.calendar.api.ICalendarSettingsPromise;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.gwt.endpoint.CalendarSettingsGwtEndpoint;
import net.bluemind.core.api.Email;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.container.api.IContainerManagementPromise;
import net.bluemind.core.container.api.IContainersPromise;
import net.bluemind.core.container.api.gwt.endpoint.ContainerManagementGwtEndpoint;
import net.bluemind.core.container.api.gwt.endpoint.ContainersGwtEndpoint;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.domain.api.gwt.endpoint.DomainSettingsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.resource.api.IResourcesPromise;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.ResourceDescriptor.PropertyValue;
import net.bluemind.resource.api.ResourceReservationMode;
import net.bluemind.resource.api.gwt.endpoint.ResourcesGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.editor.client.Editor;

public class JitsiEditor extends CompositeGwtWidgetElement {
	static final String TYPE = "bm.ac.JitsiEditor";

	private static final String RESOURCE_UID = "videoconferencing-jitsi";
	private static final String RESOURCE_CONTAINER = "videoconferencing-jitsi-settings-container";
	private static final String SETTINGS_URL = "url";
	private static final String SETTINGS_TEMPLATES = "templates";

	private static final List<String> SUPPORTED_LANGUAGES = Arrays
			.asList(new String[] { "fr", "en", "de", "es", "pt", "it", "hu", "nl", "pl", "ru", "sk", "uk", "zh" });

	private static JitsiEditorUiBinder uiBinder = GWT.create(JitsiEditorUiBinder.class);

	interface JitsiEditorUiBinder extends UiBinder<HTMLPanel, JitsiEditor> {
	}

	@UiField
	TextBox serverUrl;

	@UiField
	Editor templateEditor;

	@UiField
	ListBox templateLanguagesComboBox;

	private String domainUid;

	/** Local storage for templates. */
	private Map<String, String> templatesByLanguage = new HashMap<String, String>();

	/** Keep track of the selected template. */
	private int selectedTemplateIndex;

	protected JitsiEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new JitsiEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);

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

		resourceService.get(RESOURCE_UID).thenAccept(res -> {
			if (res != null) {
				IContainerManagementPromise containerMgmt = new ContainerManagementGwtEndpoint(
						Ajax.TOKEN.getSessionId(), RESOURCE_CONTAINER).promiseApi();
				containerMgmt.getSettings().thenAccept(settings -> {
					String url = settings.get(SETTINGS_URL);
					if (url != null) {
						serverUrl.setValue(url);
					}

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

	}

	@Override
	public void saveModel(JavaScriptObject model) {

		String url = serverUrl.asEditor().getValue();
		if (url != null) {
			IResourcesPromise resourceService = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid)
					.promiseApi();
			resourceService.get(RESOURCE_UID).thenCompose(res -> {
				if (res == null) {
					return createResource();
				}
				return CompletableFuture.completedFuture(null);
			}).thenAccept(v -> {
				setResourceSettings();
			});
		} else {
			// TOOD warning before deletion?
			removeResource();
		}

	}

	private CompletableFuture<Void> createResource() {
		ResourceDescriptor resource = new ResourceDescriptor();
		resource.label = "Jitsi";
		resource.typeIdentifier = "bm-videoconferencing"; // FIXME use IVideoConferenceUid
		resource.properties = new ArrayList<>();
		resource.properties.add(PropertyValue.create("bm-videoconferencing-type", "jitsi"));
		String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid().toLowerCase();
		resource.emails = Arrays.asList(Email.create(uid + "@" + domainUid, true, true));
		resource.reservationMode = ResourceReservationMode.AUTO_ACCEPT;

		IResourcesPromise resourceService = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		IContainersPromise containeService = new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid)
				.promiseApi();

		return resourceService.create(RESOURCE_UID, resource).thenAccept(res -> {
			IContainerManagementPromise cm = new ContainerManagementGwtEndpoint(Ajax.TOKEN.getSessionId(),
					ICalendarUids.resourceCalendar(RESOURCE_UID)).promiseApi();
			cm.setAccessControlList(Arrays.asList(AccessControlEntry.create(Ajax.TOKEN.getSubject(), Verb.All),
					AccessControlEntry.create(domainUid, Verb.Invitation)));
		}).thenAccept(res -> {
			ContainerDescriptor cd = new ContainerDescriptor();
			cd.domainUid = domainUid;
			cd.name = RESOURCE_CONTAINER;
			cd.owner = RESOURCE_UID;
			cd.type = "container_settings";
			containeService.create(RESOURCE_CONTAINER, cd);
		}).thenAccept(res -> {
			containeService.setAccessControlList(RESOURCE_CONTAINER,
					Arrays.asList(AccessControlEntry.create(Ajax.TOKEN.getSubject(), Verb.All)));
		}).thenCompose(res -> {
			return new DomainSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi().get();
		}).thenAccept(domainSettings -> {
			ICalendarSettingsPromise calendarSettingsService = new CalendarSettingsGwtEndpoint(
					Ajax.TOKEN.getSessionId(), ICalendarUids.resourceCalendar(RESOURCE_UID)).promiseApi();
			calendarSettingsService.set(createCalendarSettings(domainSettings));
		});
	}

	private void setResourceSettings() {
		IContainerManagementPromise containerMgmt = new ContainerManagementGwtEndpoint(Ajax.TOKEN.getSessionId(),
				RESOURCE_CONTAINER).promiseApi();

		Map<String, String> settings = new HashMap<>();

		String url = serverUrl.asEditor().getValue();
		settings.put(SETTINGS_URL, url);

		storeCurrentTemplate();
		String templates = JsonUtils.stringify(JsMapStringString.create(templatesByLanguage));
		settings.put(SETTINGS_TEMPLATES, templates);

		containerMgmt.setSettings(settings);
	}

	private void removeResource() {
		IResourcesPromise resourceService = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		IContainersPromise containeService = new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid)
				.promiseApi();
		containeService.delete(RESOURCE_CONTAINER).thenAccept(res -> {
			resourceService.delete(RESOURCE_UID);
		});
	}

	private CalendarSettingsData createCalendarSettings(Map<String, String> domainSettings) {
		CalendarSettingsData calSettings = new CalendarSettingsData();
		if (domainSettings.containsKey("working_days")) {
			calSettings.workingDays = getWorkingDays(domainSettings.get("working_days"));
		} else {
			calSettings.workingDays = Arrays.asList(new Day[] { Day.MO, Day.TU, Day.WE, Day.TH, Day.FR });
		}
		if (domainSettings.containsKey("timezone")) {
			calSettings.timezoneId = domainSettings.get("timezone");
		} else {
			calSettings.timezoneId = "UTC";
		}
		if (domainSettings.containsKey("work_hours_start")) {
			calSettings.dayStart = toMillisOfDay(domainSettings.get("work_hours_start"));
		} else {
			calSettings.dayStart = 9 * 60 * 60 * 1000;
		}
		if (domainSettings.containsKey("work_hours_end")) {
			calSettings.dayEnd = toMillisOfDay(domainSettings.get("work_hours_end"));
		} else {
			calSettings.dayEnd = 18 * 60 * 60 * 1000;
		}
		if (domainSettings.containsKey("min_duration")) {
			calSettings.minDuration = Math.max(60, Integer.parseInt(domainSettings.get("min_duration")));
		} else {
			calSettings.minDuration = 60;
		}
		if (!validMinDuration(calSettings.minDuration)) {
			calSettings.minDuration = 60;
		}
		return calSettings;
	}

	private boolean validMinDuration(Integer minDuration) {
		return minDuration == 60 || minDuration == 120 || minDuration == 720 || minDuration == 1440;
	}

	private Integer toMillisOfDay(String value) {
		double time = Double.parseDouble(value);
		int timeHour = (int) Double.parseDouble(value);
		int timeMinute = (int) ((time - timeHour) * 60);
		int minutes = timeHour * 60 + timeMinute;
		return minutes * 60 * 1000;
	}

	private List<Day> getWorkingDays(String string) {
		List<Day> days = new ArrayList<>();
		for (String dayString : string.split(",")) {
			switch (dayString.trim().toLowerCase()) {
			case "mon":
				days.add(Day.MO);
				break;
			case "tue":
				days.add(Day.TU);
				break;
			case "wed":
				days.add(Day.WE);
				break;
			case "thu":
				days.add(Day.TH);
				break;
			case "fri":
				days.add(Day.FR);
				break;
			case "sam":
				days.add(Day.SA);
				break;
			case "sun":
				days.add(Day.SU);
				break;
			}
		}
		return days;
	}

	private void storeCurrentTemplate() {
		String currentTemplate = templateEditor.getText();
		templatesByLanguage.put(SUPPORTED_LANGUAGES.get(selectedTemplateIndex), currentTemplate);
	}

}
