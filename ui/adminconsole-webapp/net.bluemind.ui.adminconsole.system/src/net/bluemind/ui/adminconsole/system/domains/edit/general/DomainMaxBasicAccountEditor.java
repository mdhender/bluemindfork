/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.ui.adminconsole.system.domains.edit.general;

import java.text.ParseException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;

import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.base.SubscriptionInfoHolder;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;

public class DomainMaxBasicAccountEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.DomainMaxBasicAccountEditor";
	private static final DomainConstants TEXTS = GWT.create(DomainConstants.class);

	@UiField
	IntegerBox basicAccount;

	@UiField
	HTMLPanel maxBasicAccountPanel;

	private static DomainMaxBasicAccountUiBinder uiBinder = GWT.create(DomainMaxBasicAccountUiBinder.class);

	interface DomainMaxBasicAccountUiBinder extends UiBinder<HTMLPanel, DomainMaxBasicAccountEditor> {
	}

	protected DomainMaxBasicAccountEditor(WidgetElement e) {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		basicAccount.setEnabled(!e.isReadOnly());
		maxBasicAccountPanel.setVisible(SubscriptionInfoHolder.subIncludesSimpleAccount());
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new DomainMaxBasicAccountEditor(e);
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		String max = SettingsModel.domainSettingsFrom(model)
				.getValue(DomainSettingsKeys.domain_max_basic_account.name());
		try {
			basicAccount.setValue(Integer.valueOf(max));
		} catch (NumberFormatException nfe) {
			basicAccount.setValue(null);
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		if (!SubscriptionInfoHolder.subIncludesSimpleAccount()) {
			return;
		}
		Integer value = null;
		try {
			value = basicAccount.getValueOrThrow();
		} catch (ParseException e) {
			throw new RuntimeException(TEXTS.invalidMaxBasicAccount());
		}

		if (value == null) {
			SettingsModel.domainSettingsFrom(model).remove(DomainSettingsKeys.domain_max_basic_account.name());
		} else {
			SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.domain_max_basic_account.name(),
					String.valueOf(value));
		}
	}
}
