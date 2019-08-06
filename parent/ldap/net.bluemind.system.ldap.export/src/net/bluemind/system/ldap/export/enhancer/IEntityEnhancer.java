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
package net.bluemind.system.ldap.export.enhancer;

import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.system.ldap.export.objects.DomainDirectoryGroup.MembersList;
import net.bluemind.user.api.User;

public interface IEntityEnhancer {
	Entry enhanceUser(ItemValue<Domain> domain, ItemValue<User> user, Entry entry) throws LdapException;

	List<String> userEnhancerAttributes();

	Entry enhanceGroup(ItemValue<Domain> domain, ItemValue<Group> group, MembersList members, Entry entry)
			throws LdapException;

	List<String> groupEnhancerAttributes();
}
