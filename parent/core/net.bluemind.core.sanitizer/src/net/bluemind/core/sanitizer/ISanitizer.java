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
package net.bluemind.core.sanitizer;

import java.util.Map;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.user.api.User;

/**
 * {@link ISanitizer} provides a mechanism to sanitize or set entity data
 * 
 * For instance, a {@link VCard} sanitizer could reformat phone number.
 * 
 * Sanitized entities are {@link Domain} {@link User}, {@link Group},
 * {@link VEvent} {@link VCard}, {@link ResourceDescriptor},
 * {@link ResourceTypeDescriptor} {@link Mailshare}, {@link TodoList},
 * {@link Identity}
 * 
 */
public interface ISanitizer<T> {

	public void create(T obj);

	public void update(T current, T obj);

	public default void create(T entity, Map<String, String> params) {
		create(entity);
	}

	public default void update(T current, T entity, Map<String, String> params) {
		update(current, entity);
	}
}
