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
package net.bluemind.ui.adminconsole.directory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

public interface IconTips extends Constants {

	public static final IconTips INST = GWT.create(IconTips.class);

	String iconTipUser();

	String iconTipGroup();

	String iconTipMailshare();

	String iconTipResource();

	String iconTipCalendar();

	String iconTipAddressBook();

	String iconTipGenPassword();

	String iconTipExternalUser();

}
