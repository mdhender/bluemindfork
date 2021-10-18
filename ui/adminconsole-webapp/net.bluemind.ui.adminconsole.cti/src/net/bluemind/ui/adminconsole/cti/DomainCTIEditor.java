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
package net.bluemind.ui.adminconsole.cti;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.gwt.endpoint.ExternalSystemGwtEndpoint;
import net.bluemind.ui.adminconsole.cti.l10n.DomainCTIEditorConstants;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.common.client.forms.TrPanel;

public class DomainCTIEditor extends CompositeGwtWidgetElement {
	static final String TYPE = "bm.ac.DomainCTIEditor";

	private static DomainCTIEditorUiBinder uiBinder = GWT.create(DomainCTIEditorUiBinder.class);

	interface DomainCTIEditorUiBinder extends UiBinder<HTMLPanel, DomainCTIEditor> {
	}

	private int wazoIndex = -1;
	private int xivoIndex = -1;

	@UiField
	TrPanel choicePanel;

	@UiField
	HTMLPanel xivoParams;

	@UiField
	HTMLPanel wazoParams;

	@UiField
	TextBox wazoHost;

	@UiField
	TextBox xivoHost;

	private ListBox ctiTypeSel;

	SettingsModel domainSettings;

	protected DomainCTIEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		ctiTypeSel = new ListBox();
		ctiTypeSel.addItem("----");
		choicePanel.add(new Label(DomainCTIEditorConstants.INST.implementation()), "label");
		choicePanel.add(ctiTypeSel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new DomainCTIEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		wazoParams.setVisible(false);
		xivoParams.setVisible(false);
		domainSettings = SettingsModel.domainSettingsFrom(model);

		ExternalSystemGwtEndpoint service = new ExternalSystemGwtEndpoint(Ajax.TOKEN.getSessionId());
		service.getExternalSystems(new DefaultAsyncHandler<List<ExternalSystem>>() {

			@Override
			public void success(List<ExternalSystem> value) {
				int index = 1;
				for (ExternalSystem system : value) {
					switch (system.identifier) {
					case "Wazo":
						ctiTypeSel.addItem("Wazo");
						wazoIndex = index++;
						break;
					case "Xivo":
						ctiTypeSel.addItem("Xivo");
						xivoIndex = index++;
						break;
					default:
					}
				}
				ctiTypeSel.addChangeHandler(event -> {
					if (wazoIndex != -1) {
						wazoParams.setVisible(ctiTypeSel.getSelectedIndex() == wazoIndex);
					}
					if (xivoIndex != -1) {
						xivoParams.setVisible(ctiTypeSel.getSelectedIndex() == xivoIndex);
					}
				});

				String ctiImplementation = domainSettings.get(DomainSettingsKeys.cti_implementation.name());
				if (ctiImplementation != null) {
					String host = domainSettings.get(DomainSettingsKeys.cti_host.name());
					switch (ctiImplementation) {
					case "Wazo":
						if (wazoIndex != -1) {
							wazoHost.setValue(host);
							ctiTypeSel.setSelectedIndex(wazoIndex);
							wazoParams.setVisible(true);
							xivoParams.setVisible(false);
						}
						break;
					case "Xivo":
						if (xivoIndex != -1) {
							xivoHost.setValue(host);
							ctiTypeSel.setSelectedIndex(xivoIndex);
							xivoParams.setVisible(true);
							wazoParams.setVisible(false);
						}
						break;
					}
				}

			}
		});

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		int selectedIndex = ctiTypeSel.getSelectedIndex();
		if (selectedIndex == 0) {
			domainSettings.remove(DomainSettingsKeys.cti_implementation.name());
		} else if (selectedIndex == wazoIndex) {
			domainSettings.putString(DomainSettingsKeys.cti_implementation.name(), "Wazo");
			domainSettings.putString(DomainSettingsKeys.cti_host.name(), wazoHost.getText());

		} else if (selectedIndex == xivoIndex) {
			domainSettings.putString(DomainSettingsKeys.cti_implementation.name(), "Xivo");
			domainSettings.putString(DomainSettingsKeys.cti_host.name(), xivoHost.getText());
		}
	}

}
