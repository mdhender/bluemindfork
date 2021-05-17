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

public class DomainMaxVisioAccountEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.DomainMaxVisioAccountEditor";
	private static final DomainConstants TEXTS = GWT.create(DomainConstants.class);

	@UiField
	IntegerBox visioAccount;

	@UiField
	HTMLPanel maxVisioAccountPanel;

	private static DomainMaxVisioAccountUiBinder uiBinder = GWT.create(DomainMaxVisioAccountUiBinder.class);

	interface DomainMaxVisioAccountUiBinder extends UiBinder<HTMLPanel, DomainMaxVisioAccountEditor> {
	}

	protected DomainMaxVisioAccountEditor(WidgetElement e) {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		visioAccount.setEnabled(!e.isReadOnly());
		maxVisioAccountPanel.setVisible(SubscriptionInfoHolder.subIncludesVisioAccount());
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new DomainMaxVisioAccountEditor(e);
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		String max = SettingsModel.domainSettingsFrom(model)
				.getValue(DomainSettingsKeys.domain_max_fullvisio_accounts.name());
		try {
			visioAccount.setValue(Integer.valueOf(max));
		} catch (NumberFormatException nfe) {
			visioAccount.setValue(null);
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		if (!SubscriptionInfoHolder.subIncludesVisioAccount()) {
			return;
		}
		Integer value = null;
		try {
			value = visioAccount.getValueOrThrow();
		} catch (ParseException e) {
			throw new RuntimeException(TEXTS.invalidMaxVisioAccount());
		}

		if (value == null) {
			SettingsModel.domainSettingsFrom(model).remove(DomainSettingsKeys.domain_max_fullvisio_accounts.name());
		} else {
			SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.domain_max_fullvisio_accounts.name(),
					String.valueOf(value));
		}
	}
}
