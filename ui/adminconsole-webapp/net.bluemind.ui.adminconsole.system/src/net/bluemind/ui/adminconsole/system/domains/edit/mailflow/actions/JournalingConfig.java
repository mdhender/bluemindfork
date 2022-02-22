/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.ui.adminconsole.system.domains.edit.mailflow.actions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.admin.client.forms.MultiStringEditContainer;
import net.bluemind.ui.adminconsole.base.ui.MailAddress;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.exceptions.InvalidEmailException;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.exceptions.MailflowException;
import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;

public class JournalingConfig extends Composite implements MailflowActionConfig {
	private static final DomainConstants TEXTS = GWT.create(DomainConstants.class);

	private static final String EMAILS_DELIMITER = ";";

	Grid tbl = new Grid();

	public JournalingConfig() {
		tbl = new Grid(4, 1);
		tbl.setCellPadding(10);
		tbl.setWidget(0, 0, new Label(TEXTS.addTargetEmail()));
		TextBox targetEmail = new TextBox();
		tbl.setWidget(1, 0, targetEmail);

		tbl.setWidget(2, 0, new Label(TEXTS.addEmailsFiltered()));
		MultiStringEditContainer emailsFiltered = new MultiStringEditContainer();
		tbl.setWidget(3, 0, emailsFiltered);

		this.initWidget(tbl);
	}

	@Override
	public Map<String, String> get() throws MailflowException {
		Map<String, String> values = new HashMap<>();

		String targetEmailValue = ((TextBox) tbl.getWidget(1, 0)).getValue();
		checkEmail(targetEmailValue);
		values.put("targetEmail", targetEmailValue);

		Set<String> filteredEmailsList = ((MultiStringEditContainer) tbl.getWidget(3, 0)).getValues();
		for (String filteredEmailValue : filteredEmailsList) {
			checkEmail(filteredEmailValue);
		}
		values.put("emailsFiltered", filteredEmailsList.stream().collect(Collectors.joining(EMAILS_DELIMITER)));

		return values;
	}

	private void checkEmail(String email) throws InvalidEmailException {
		if (!MailAddress.isValid(email)) {
			throw new InvalidEmailException(TEXTS.invalidEmail(email));
		}
	}

	@Override
	public void set(Map<String, String> config) {
		((TextBox) tbl.getWidget(1, 0)).setValue(config.get("targetEmail"));
		String emailsFilteredConfig = config.get("emailsFiltered");
		if (emailsFilteredConfig != null && !emailsFilteredConfig.isEmpty()) {
			String[] emailsFilteredTab = emailsFilteredConfig.split(EMAILS_DELIMITER);
			((MultiStringEditContainer) tbl.getWidget(3, 0)).setValues(new HashSet<>(Arrays.asList(emailsFilteredTab)));
		}
	}

	@Override
	public String getIdentifier() {
		return "JournalingAction";
	}

	@Override
	public Widget getWidget() {
		return this;
	}
}
