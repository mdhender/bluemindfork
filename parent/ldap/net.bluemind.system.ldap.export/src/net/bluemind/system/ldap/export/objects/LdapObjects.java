/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.ldap.export.objects;

import java.util.Iterator;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.message.ModifyRequest;

import net.bluemind.core.api.fault.ServerFault;

public abstract class LdapObjects {
	public abstract String getDn();

	public abstract String getRDn();

	public abstract Entry getLdapEntry() throws ServerFault;

	public ModifyRequest getModifyRequest(Entry currentEntry) throws ServerFault {
		return null;
	}

	protected ModifyRequest updateLdapAttribute(ModifyRequest modifyRequest, Entry currentEntry, Entry entry,
			String attributeName) {
		Attribute currentAttribute = currentEntry.get(attributeName);
		Attribute attribute = entry.get(attributeName);

		if (currentAttribute == null && attribute == null) {
			return modifyRequest;
		}

		if (currentAttribute == null && attribute != null) {
			return modifyRequest.add(attribute);
		}

		if (currentAttribute != null && attribute == null) {
			return modifyRequest.remove(currentAttribute);
		}

		if (currentAttribute.size() != attribute.size()) {
			return modifyRequest.replace(attribute);
		}

		Iterator<Value<?>> it = attribute.iterator();
		while (it.hasNext()) {
			Value<?> val = it.next();
			if (!currentAttribute.contains(val)) {
				return modifyRequest.replace(attribute);
			}
		}

		return modifyRequest;
	}
}
