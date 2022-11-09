/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.deferredaction.api;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.MoreObjects;

import jakarta.validation.constraints.NotNull;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Required;

/**
 * {@link DeferredAction} describes an action which will be executed at a
 * specific date.
 */
@BMApi(version = "3")
public class DeferredAction {

	/**
	 * actionId associates the action with a specific plugin handling this type of
	 * actions
	 */
	@Required
	@NotNull
	public String actionId;

	/**
	 * reference associates the action with a specific entity (for example, a series
	 * of actions referencing a single event)
	 */
	@Required
	@NotNull
	public String reference;

	/**
	 * The action execution date
	 */
	@Required
	@NotNull
	public Date executionDate;

	/**
	 * Generic action configuration
	 */
	public Map<String, String> configuration = new HashMap<>();

	public DeferredAction copy(Date executionDate) {
		DeferredAction copy = new DeferredAction();
		copy.actionId = actionId;
		copy.reference = reference;
		copy.configuration = configuration;
		copy.executionDate = executionDate;
		return copy;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(DeferredAction.class)//
				.add("actionId", actionId)//
				.add("reference", reference)//
				.add("executionDate", executionDate)//
				.add("configuration", configuration)//
				.toString();

	}
}
