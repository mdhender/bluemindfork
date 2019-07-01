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
package net.bluemind.ui.common.client.forms.extensions;

import java.util.Map;

import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.common.client.forms.IFormChangeListener;

public interface ICommonEditor {
	void setTitleText(String s);

	String getStringValue();

	void setStringValue(String v);

	void setDescriptionText(String s);

	Map<String, Widget> getWidgetsMap();

	void setPropertyName(String string);

	Widget asWidget();

	String getPropertyName();

	void setReadOnly(boolean readOnly);

	void addFormChangeListener(IFormChangeListener listener);

	void setId(String id);
}
