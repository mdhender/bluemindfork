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
package net.bluemind.ui.adminconsole.directory.resourcetype.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

public interface ResourceTypeConstants extends Constants {

	public static final ResourceTypeConstants INST = GWT.create(ResourceTypeConstants.class);

	String create();

	String delete();

	String all();

	String selectColumn();

	String label();

	String availability();

	String domainColumn();

	String deleteTypeUsedByResources();

	String deleteConfirmation();

	String massDeleteConfirmation();

	String addFilter();

	String generalTab();

	String mon();

	String tue();

	String wed();

	String thu();

	String fri();

	String sat();

	String sun();

	String none();

	String addCustomProp();

	String type();

	String customPropBoolean();

	String customPropInteger();

	String customPropText();

	String emptyLabel();

	String templatePreviewButtonOn();

	String templatePreviewButtonOff();
}
