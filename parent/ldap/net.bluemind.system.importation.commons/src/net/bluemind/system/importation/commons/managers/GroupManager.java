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
package net.bluemind.system.importation.commons.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.lib.ldap.GroupMemberAttribute;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.enhancer.GroupData;
import net.bluemind.system.importation.commons.enhancer.IEntityEnhancer;
import net.bluemind.system.importation.commons.scanner.IImportLogger;
import net.bluemind.system.importation.commons.scanner.ImportLogger;

public abstract class GroupManager extends EntityManager {
	private static final Logger logger = LoggerFactory.getLogger(GroupManager.class);

	protected boolean create = true;
	public final Entry entry;

	public ItemValue<Group> group = ItemValue.create(Item.create(null, null), new Group());

	public GroupManager(ItemValue<Domain> domain, Entry entry) {
		super(domain);
		this.entry = entry;
	}

	public abstract String getExternalId(IImportLogger importLogger);

	protected abstract String getNameFromDefaultAttribute(IImportLogger importLogger);

	protected abstract void manageInfos() throws LdapInvalidAttributeValueException;

	protected abstract List<String> getEmails();

	protected abstract List<IEntityEnhancer> getEntityEnhancerHooks();

	protected abstract Parameters getDirectoryParameters();

	/**
	 * https://docs.microsoft.com/en-us/previous-versions/windows/desktop/ldap/searching-using-range-retrieval
	 * 
	 * @param groupMembersAttribute
	 * @return Set of member DN
	 */
	protected abstract Set<String> getRangedGroupMembers();

	public void update(ItemValue<Group> currentGroup) throws ServerFault {
		update(new ImportLogger(), currentGroup);
	}

	public void update(IImportLogger importLogger, ItemValue<Group> currentGroup) {
		if (currentGroup != null) {
			group = currentGroup;
			create = false;
		}

		doUpdate(importLogger);
	}

	private void doUpdate(IImportLogger importLogger) {
		try {
			if (create) {
				group.uid = UIDGenerator.uid();
				group.externalId = getExternalId(importLogger);
			}

			group.value.name = getName(importLogger);

			manageInfos();

			manageEmails(getEmails());

			GroupData pluginGroup = new GroupData() {
				@Override
				public String getUid() {
					return GroupManager.this.group.uid;
				}
			};

			pluginGroup.group = group.value;

			for (IEntityEnhancer iee : getEntityEnhancerHooks()) {
				pluginGroup = iee.enhanceGroup(importLogger.withoutStatus(), getDirectoryParameters(), domain, entry,
						pluginGroup);
			}

			group.value = pluginGroup.group;
		} catch (LdapInvalidAttributeValueException e) {
			throw new ServerFault(e);
		}
	}

	private String getName(IImportLogger importLogger) {
		Optional<String> groupName = Optional.empty();
		for (IEntityEnhancer iee : getEntityEnhancerHooks()) {
			groupName = iee.getGroupName(importLogger.withoutStatus(), getDirectoryParameters(), domain, entry)
					.map(name -> name.trim().isEmpty() ? null : name);
		}

		return groupName.orElse(getNameFromDefaultAttribute(importLogger));
	}

	protected void manageEmails(List<String> groupEmails) {
		List<Email> emails = new LinkedList<>();

		Map<String, Set<String>> localEmails = groupEmails.stream().filter(groupEmail -> isLocalEmail(groupEmail))
				.collect(Collectors.toMap(this::getEmailLeftPart, this::getEmailRightParts, this::mergeEmailRightParts,
						HashMap::new));
		if (!localEmails.isEmpty()) {
			String defaultLocalEmail = getDefaultLocalEmail(groupEmails);

			Set<String> domainAliases = getDomainAliases();

			localEmails.entrySet().stream()
					.filter(localEmailEntry -> localEmailEntry.getValue().size() == domainAliases.size())
					.forEach(localEmailEntry -> emails.add(Email.create(
							defaultLocalEmail.startsWith(localEmailEntry.getKey() + "@") ? defaultLocalEmail
									: localEmailEntry.getKey() + "@" + domain.value.name,
							defaultLocalEmail.startsWith(localEmailEntry.getKey() + "@"), true)));

			localEmails.entrySet().stream()
					.filter(localEmailEntry -> localEmailEntry.getValue().size() != domainAliases.size()).forEach(
							localEmailEntry -> localEmailEntry.getValue()
									.forEach(domain -> emails.add(Email.create(localEmailEntry.getKey() + "@" + domain,
											defaultLocalEmail.equals(localEmailEntry.getKey() + "@" + domain),
											false))));
		}

		if (logger.isDebugEnabled()) {
			emails.stream().forEach(e -> logger.debug(e.address + " def:" + e.isDefault + " allalias:" + e.allAliases));
		}

		group.value.emails = emails;
	}

	public Set<String> getGroupMembers(GroupMemberAttribute groupMembersAttribute) {
		Attribute member = entry.get(groupMembersAttribute.name());
		if (member != null && member.size() != 0) {
			return getGroupMembers(member);
		}

		return getRangedGroupMembers();
	}

	protected Set<String> getGroupMembers(Attribute member) {
		Set<String> groupMembers = new HashSet<>();

		Iterator<Value<?>> iterator = member.iterator();
		while (iterator.hasNext()) {
			String memberValue = iterator.next().getString();
			if (memberValue != null && !memberValue.trim().isEmpty()) {
				groupMembers.add(memberValue);
			}
		}

		return groupMembers;
	}

	public boolean isSplitDomainGroup(IImportLogger importLogger) {
		Parameters parameters = getDirectoryParameters();
		if (!parameters.splitDomain.splitRelayEnabled) {
			return false;
		}

		if (Strings.isNullOrEmpty(parameters.splitDomain.relayMailboxGroup)
				|| !parameters.splitDomain.relayMailboxGroup.equals(getName(importLogger))) {
			return false;
		}

		return true;
	}
}
