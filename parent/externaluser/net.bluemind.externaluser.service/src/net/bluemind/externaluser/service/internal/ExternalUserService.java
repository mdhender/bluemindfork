/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.externaluser.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.utils.ValidationResult;
import net.bluemind.directory.service.DirDomainValue;
import net.bluemind.directory.service.DirEventProducer;
import net.bluemind.domain.api.Domain;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.externaluser.service.IInCoreExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.persistence.GroupStore;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.BasicRoles;

public class ExternalUserService implements IInCoreExternalUser {

	final static Logger logger = LoggerFactory.getLogger(ExternalUserService.class);
	private ExternalUserContainerStoreService storeService;
	private RBACManager rbacManager;
	private ExternalUserValidator validator;
	private Sanitizer sanitizer;
	private String domainUid;
	private BmContext bmContext;
	private GroupStore groupStore;
	private Container externalUserContainer;
	private IGroup groupService;
	private DirEventProducer eventProducer;

	public ExternalUserService(BmContext context, ItemValue<Domain> domain, Container externalUserContainer) {
		storeService = new ExternalUserContainerStoreService(context, domain, externalUserContainer);
		rbacManager = new RBACManager(context).forDomain(domain.uid);
		validator = new ExternalUserValidator();
		sanitizer = new Sanitizer(context);
		domainUid = domain.uid;
		bmContext = context;
		groupStore = new GroupStore(context.getDataSource(), externalUserContainer);
		this.externalUserContainer = externalUserContainer;
		this.groupService = bmContext.provider().instance(IGroup.class, domainUid);
		this.eventProducer = new DirEventProducer(domainUid, VertxPlatform.eventBus());
	}

	@Override
	public void create(String uid, ExternalUser eu) throws ServerFault {
		createWithExtId(uid, null, eu);
	}

	@Override
	public void createWithExtId(String uid, String extId, ExternalUser externalUser) throws ServerFault {
		rbacManager.forOrgUnit(externalUser.orgUnitUid).check(BasicRoles.ROLE_MANAGE_EXTERNAL_USER);
		ParametersValidator.notNullAndNotEmpty(uid);

		sanitizer.create(externalUser);
		sanitizer.create(new DirDomainValue<>(domainUid, uid, externalUser));
		validator.validate(externalUser, uid, domainUid, bmContext);

		storeService.createWithExtId(uid, extId, externalUser);
		eventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public void update(String uid, ExternalUser externalUser) throws ServerFault {
		rbacManager.forOrgUnit(externalUser.orgUnitUid).check(BasicRoles.ROLE_MANAGE_EXTERNAL_USER);
		ParametersValidator.notNullAndNotEmpty(uid);

		ExternalUser previous = storeService.get(uid).value;

		sanitizer.update(previous, externalUser);
		sanitizer.update(new DirDomainValue<>(domainUid, uid, previous),
				new DirDomainValue<>(domainUid, uid, externalUser));
		validator.validate(externalUser, uid, domainUid, bmContext);

		storeService.update(uid, externalUser);
		eventProducer.changed(uid, storeService.getVersion());
	}

	@Override
	public void delete(String externalUserUid) throws ServerFault {
		ItemValue<ExternalUser> extUser = getComplete(externalUserUid);
		String ou = extUser != null ? extUser.value.orgUnitUid : null;
		rbacManager.forOrgUnit(ou).check(BasicRoles.ROLE_MANAGE_EXTERNAL_USER);
		ParametersValidator.notNullAndNotEmpty(externalUserUid);

		// remove group memberships
		Member m = Member.externalUser(externalUserUid);
		for (String groupUid : memberOfGroups(externalUserUid)) {
			groupService.remove(groupUid, Arrays.asList(m));
		}

		storeService.delete(externalUserUid);
		eventProducer.deleted(externalUserUid, storeService.getVersion());
	}

	@Override
	public ItemValue<ExternalUser> getComplete(String uid) throws ServerFault {
		ParametersValidator.notNullAndNotEmpty(uid);

		ItemValue<ExternalUser> item = storeService.get(uid);
		if (item == null) {
			return null;
		}
		rbacManager.forOrgUnit(item.value.orgUnitUid).check(BasicRoles.ROLE_MANAGE_EXTERNAL_USER);
		return item;
	}

	@Override
	public ValidationResult validate(String[] externalUserUids) throws ServerFault {
		boolean valid = storeService.allValid(externalUserUids);
		if (valid) {
			return new ValidationResult(valid, externalUserUids);
		} else {
			Map<String, Boolean> validationResults = new HashMap<>();
			for (String uid : externalUserUids) {
				validationResults.put(uid, storeService.allValid(new String[] { uid }));
			}
			return new ValidationResult(valid, validationResults);
		}
	}

	@Override
	public List<ItemValue<Group>> memberOf(String uid) throws ServerFault {
		ItemValue<ExternalUser> extUser = getComplete(uid);
		String ou = extUser != null ? extUser.value.orgUnitUid : null;
		rbacManager.forOrgUnit(ou).check(BasicRoles.ROLE_MANAGE_EXTERNAL_USER, BasicRoles.ROLE_MANAGE_GROUP_MEMBERS);

		List<String> groupsUid = memberOfGroupUid(uid);

		ArrayList<ItemValue<Group>> groups = new ArrayList<ItemValue<Group>>();

		for (String groupUid : groupsUid) {
			groups.add(groupService.getComplete(groupUid));
		}

		return groups;
	}

	@Override
	public List<String> memberOfGroups(String uid) throws ServerFault {
		ItemValue<ExternalUser> extUser = getComplete(uid);
		String ou = extUser != null ? extUser.value.orgUnitUid : null;
		rbacManager.forOrgUnit(ou).check(BasicRoles.ROLE_MANAGE_EXTERNAL_USER, BasicRoles.ROLE_MANAGE_GROUP_MEMBERS);

		return memberOfGroupUid(uid);
	}

	private List<String> memberOfGroupUid(String uid) throws ServerFault {
		Item item = null;

		try {
			item = storeService.getItemStore().get(uid);
		} catch (SQLException sqle) {
			logger.error("Fail to get item {}", uid, sqle);
			throw new ServerFault(sqle);
		}

		if (item == null) {
			logger.debug("Invalid user UID: " + uid);
			throw new ServerFault("Invalid user UID: " + uid);
		}

		try {
			return groupStore.getUserGroups(externalUserContainer, item);
		} catch (SQLException e) {
			logger.error("Unable to get groups for user {}", uid, e);
			throw ServerFault.sqlFault(e);
		}

	}

	@Override
	public ItemValue<ExternalUser> byExtId(String extId) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_USER);
		ParametersValidator.notNullAndNotEmpty(extId);

		return storeService.findByExtId(extId);
	}
}
