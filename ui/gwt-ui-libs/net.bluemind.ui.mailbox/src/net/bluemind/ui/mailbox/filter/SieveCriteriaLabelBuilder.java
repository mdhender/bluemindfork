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
package net.bluemind.ui.mailbox.filter;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.utils.RuleHandler;
import net.bluemind.mailbox.api.utils.RuleParser;
import net.bluemind.ui.mailbox.filter.SieveEdit.Style;

/**
 * Create label from Sieve rule {@link SieveFilter}
 * 
 * @author laurentb
 *
 */
public class SieveCriteriaLabelBuilder {

	private Style s;
	private SieveConstants constants;

	public SieveCriteriaLabelBuilder(SieveEdit.Style style, SieveConstants constants) {
		this.s = style;
		this.constants = constants;
	}

	public FlexTable buildCriteria(MailFilterRule rule) {

		final FlexTable criteria = new FlexTable();
		criteria.setStyleName(s.filterCriteria());
		RuleParser.visit(rule, new RuleHandler() {

			@Override
			public void matches(String field, String value) {
			}

			@Override
			public void isNot(String field, String value) {
				createCriterion(field, constants.isNot(), value);
			}

			@Override
			public void is(String field, String value) {
				createCriterion(field, constants.is(), value);
			}

			@Override
			public void exists(String field) {
				createCriterion(field, constants.exists(), "");
			}

			@Override
			public void doesnotMatch(String field, String value) {

			}

			@Override
			public void doesnotExist(String field) {
				createCriterion(field, constants.doesNotExist(), "");

			}

			@Override
			public void doesnotContain(String field, String value) {
				createCriterion(field, constants.doesNotContain(), value);
			}

			@Override
			public void contains(String field, String value) {
				createCriterion(field, constants.contains(), value);

			}

			private void createCriterion(String crit, String smatch, String value) {

				Label lcrit = null;
				FlexTable customCrit = null;

				if (crit.equals("FROM")) {
					lcrit = new Label(constants.from() + " " + smatch);
				} else if (crit.equals("TO")) {
					lcrit = new Label(constants.to() + " " + smatch);
				} else if (crit.equals("SUBJECT")) {
					lcrit = new Label(constants.subject() + " " + smatch);
				} else if (crit.equals("BODY")) {
					lcrit = new Label(constants.body() + " " + smatch);
				} else {
					customCrit = new FlexTable();
					customCrit.setWidget(0, 0, new Label(constants.header()));
					customCrit.setWidget(0, 1, new Label(crit));
					customCrit.setWidget(0, 2, new Label(smatch));
					customCrit.getCellFormatter().setStyleName(0, 1, s.value());
				}
				int r = criteria.getRowCount();

				if (lcrit != null) {
					criteria.setWidget(r, 0, lcrit);
				} else if (customCrit != null) {
					criteria.setWidget(r, 0, customCrit);
				} else {
				}

				criteria.setWidget(r, 1, new Label(value));
				criteria.getCellFormatter().setStyleName(r, 0, s.label());
				criteria.getCellFormatter().setStyleName(r, 1, s.value());

			}
		});

		return criteria;
	}
}
