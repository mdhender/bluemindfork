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

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.ConstantsWithLookup;

public interface SieveConstants extends ConstantsWithLookup {
	public static final SieveConstants INST = GWT.create(SieveConstants.class);

	String update();

	String newFilter();

	String modifyFilter();

	String myFilters();

	String domainFilters();

	String from();

	String to();

	String subject();

	String body();

	String markAsRead();

	String markAsImportant();

	String delete();

	String moveTo();

	String forwardTo();

	String forwardToWithLocalCopy();

	String forwardToWithoutLocalCopy();

	String addFilterBtn();

	String modifyFilterBtn();

	String cancelBtn();

	String criteria();

	String action();

	String active();

	String noFilterFound();

	String inbox();

	String sent();

	String trash();

	String drafts();

	String spam();

	String is();

	String isNot();

	String contains();

	String doesNotContain();

	String sieveFilters();

	String header();

	String exists();

	String doesNotExist();

	String discard();
}
