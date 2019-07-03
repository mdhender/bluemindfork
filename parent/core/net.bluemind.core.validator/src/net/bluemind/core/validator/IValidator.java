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
package net.bluemind.core.validator;

import java.util.Map;

/**
 * {@link IValidator} provides a mechanism to validate entity data
 * 
 * For instance, a {@link VCard} validator could throw an error on invalid phone
 * number
 * 
 */
public interface IValidator<T> {

	public void create(T obj);

	public void update(T oldValue, T newValue);

	public default void create(T obj, Map<String, String> params) {
		create(obj);
	}

	public default void update(T oldValue, T newValue, Map<String, String> params) {
		update(oldValue, newValue);
	}
}
