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
package net.bluemind.ui.editor.client;

import com.google.gwt.core.client.JavaScriptObject;

public class JSEditor extends JavaScriptObject {

	protected JSEditor() {

	}

	public native final String getValue() /*-{
											return this.getValue();
											}-*/;

	public native final void setValue(String html) /*-{
													this.setValue(html);
													}-*/;

	public native final void textarea() /*-{
										this.textarea();
										}-*/;

	public native final void composer() /*-{
										this.composer();
										}-*/;
}
