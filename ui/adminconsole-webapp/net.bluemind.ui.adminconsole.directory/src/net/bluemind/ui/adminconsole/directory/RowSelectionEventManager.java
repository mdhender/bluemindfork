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

import com.google.gwt.view.client.DefaultSelectionEventManager;

import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;

/**
 * 
 * SelectionEventManager which handle select box and row
 * 
 * @param <T>
 */
public class RowSelectionEventManager<T> extends DefaultSelectionEventManager<T> {

	protected RowSelectionEventManager(
			com.google.gwt.view.client.DefaultSelectionEventManager.EventTranslator<T> translator) {
		super(translator);
	}

	public static <T> RowSelectionEventManager<T> createRowManager(ScreenShowRequest ssr) {
		return new RowSelectionEventManager<T>(new RowEventTranslator<T>(ssr));
	}
}
