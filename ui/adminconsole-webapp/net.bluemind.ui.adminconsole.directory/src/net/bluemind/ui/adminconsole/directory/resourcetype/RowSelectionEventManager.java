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
package net.bluemind.ui.adminconsole.directory.resourcetype;

import com.google.gwt.view.client.DefaultSelectionEventManager;

import net.bluemind.resource.api.type.ResourceType;

/**
 * 
 * SelectionEventManager which handle select box and row
 * 
 * @param <ResourceType>
 */
public class RowSelectionEventManager extends DefaultSelectionEventManager<ResourceType> {

	protected RowSelectionEventManager(
			com.google.gwt.view.client.DefaultSelectionEventManager.EventTranslator<ResourceType> translator) {
		super(translator);
	}

	public static RowSelectionEventManager createRowManager() {
		return new RowSelectionEventManager(new RowEventTranslator());
	}

}
