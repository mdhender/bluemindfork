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
package net.bluemind.ui.adminconsole.system.systemconf.eas;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ui.adminconsole.system.systemconf.SysConfModel;
import net.bluemind.ui.adminconsole.system.systemconf.util.ValueUtil;

public class SysConfEasServerEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.SysConfEasServerEditor";

	private static final int DEFAULT_MIN_PUSH_TIMEOUT = 120;
	private static final int DEFAULT_MAX_PUSH_TIMEOUT = 1200;

	@UiField
	CheckBox syncUnknownDevices;

	@UiField
	IntegerBox minPushTimeout;

	@UiField
	IntegerBox maxPushTimeout;

	private static SysConfEasServerUiBinder uiBinder = GWT.create(SysConfEasServerUiBinder.class);

	interface SysConfEasServerUiBinder extends UiBinder<HTMLPanel, SysConfEasServerEditor> {
	}

	protected SysConfEasServerEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new SysConfEasServerEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		SysConfModel map = SysConfModel.from(model);
		String syncUnknown = null;
		if (map.getValue(SysConfKeys.eas_sync_unknown.name()) != null) {
			syncUnknown = map.getValue(SysConfKeys.eas_sync_unknown.name()).toString();
		}
		syncUnknownDevices.setValue(ValueUtil.readBooleanValue(syncUnknown));
		minPushTimeout.setText(
				ValueUtil.readIntValue(map.getValue(SysConfKeys.eas_min_heartbeat.name()), DEFAULT_MIN_PUSH_TIMEOUT));
		maxPushTimeout.setText(
				ValueUtil.readIntValue(map.getValue(SysConfKeys.eas_max_heartbeat.name()), DEFAULT_MAX_PUSH_TIMEOUT));
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		String sanitizedMinPushTimeout = ValueUtil.removeNonDigitCharacters(minPushTimeout.getText().toString(),
				DEFAULT_MIN_PUSH_TIMEOUT);
		String sanitizedMaxPushTimeout = ValueUtil.removeNonDigitCharacters(maxPushTimeout.getText().toString(),
				DEFAULT_MAX_PUSH_TIMEOUT);

		SysConfModel map = SysConfModel.from(model);

		map.setValue(SysConfKeys.eas_sync_unknown.name(), syncUnknownDevices.getValue().toString());
		map.setValue(SysConfKeys.eas_min_heartbeat.name(), sanitizedMinPushTimeout);
		map.setValue(SysConfKeys.eas_max_heartbeat.name(), sanitizedMaxPushTimeout);
		minPushTimeout.setText(sanitizedMinPushTimeout);
		maxPushTimeout.setText(sanitizedMaxPushTimeout);
	}

}
