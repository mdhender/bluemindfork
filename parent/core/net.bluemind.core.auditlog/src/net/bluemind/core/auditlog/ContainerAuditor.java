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
package net.bluemind.core.auditlog;

import net.bluemind.core.container.model.Container;

public class ContainerAuditor<V extends ContainerAuditor<V>> extends Auditor<V> {

	public ContainerAuditor(IAuditManager manager) {
		super(manager);
	}

	private static final String CONTAINER = "container-json";

	public V forContainer(Container container) {
		object(container.uid + "@" + container.domainUid);
		addObjectMetadata(CONTAINER, container);
		return (V) getThis();
	}

	public V actionItemUid(String uid) {
		addActionMetadata("item-uid", uid);
		return getThis();
	}

	public V actionValue(Object value) {
		if (value != null) {
			return addActionMetadata("value", value);
		} else {
			return addActionMetadata("value", null);
		}
	}

	public V actionValueSanitized(Object value) {
		if (value != null) {
			return addActionMetadata("sanitized-value", value);
		} else {
			return addActionMetadata("sanitized-value", null);
		}
	}

	public V previousValue(Object value) {
		if (value != null) {
			return addActionMetadata("previous-value", value);
		} else {
			return addActionMetadata("previous-value", null);
		}
	}

	public V actionCreateOn(String uid) {
		return action("create").actionItemUid(uid);
	}

	public V actionUpdateOn(String uid) {
		return action("update").actionItemUid(uid);
	}

	public V actionDeleteOn(String uid) {
		return action("delete").actionItemUid(uid);
	}
}
