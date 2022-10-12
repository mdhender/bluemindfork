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
package net.bluemind.system.ldap.importation.tests.enhancer;

import java.util.Arrays;
import java.util.Optional;

import org.apache.directory.api.ldap.model.entry.Entry;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.enhancer.GroupData;
import net.bluemind.system.importation.commons.enhancer.IEntityEnhancer;
import net.bluemind.system.importation.commons.enhancer.UserData;
import net.bluemind.system.importation.commons.scanner.IImportLogger;

public class EntityEnhancerHook implements IEntityEnhancer {
	@Override
	public UserData enhanceUser(IImportLogger importLogger, Parameters ldapParameters, ItemValue<Domain> domain,
			Entry entry, UserData userData) {
		userData.user.dataLocation = "hook value";

		MailFilterRule r = new MailFilterRule();
		r.addDiscard();
		userData.mailFilter.rules = Arrays.asList(r);
		return userData;
	}

	@Override
	public GroupData enhanceGroup(IImportLogger importLogger, Parameters ldapParameters, ItemValue<Domain> domain,
			Entry entry, GroupData groupData) {
		groupData.group.dataLocation = "hook value";
		return groupData;
	}

	@Override
	public Optional<String> getUserLogin(IImportLogger importLogger, Parameters directoryParameters,
			ItemValue<Domain> domain, Entry entry) {
		return Optional.empty();
	}

	@Override
	public Optional<String> getGroupName(IImportLogger importLogger, Parameters directoryParameters,
			ItemValue<Domain> domain, Entry entry) {
		return Optional.empty();
	}
}
