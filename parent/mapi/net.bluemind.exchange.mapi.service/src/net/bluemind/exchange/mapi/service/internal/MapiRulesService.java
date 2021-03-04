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
package net.bluemind.exchange.mapi.service.internal;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.exchange.mapi.api.IMapiRules;
import net.bluemind.exchange.mapi.api.MapiRule;
import net.bluemind.exchange.mapi.api.MapiRuleChanges;
import net.bluemind.exchange.mapi.persistence.MapiRuleStore;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class MapiRulesService implements IMapiRules {

	private static final Logger logger = LoggerFactory.getLogger(MapiRulesService.class);
	private final RBACManager rbacManager;
	private final MapiRuleStore ruleStore;
	private BmContext context;

	public MapiRulesService(BmContext context, DataSource dataSource, String containerUid) {
		this.context = context;
		this.ruleStore = new MapiRuleStore(dataSource, containerUid);
		IContainers contApi = context.su().provider().instance(IContainers.class);
		BaseContainerDescriptor desc = contApi.getContainersLight(Arrays.asList(containerUid)).get(0);
		String aclContainer = containerUid;
		if (desc.type.equals(IMailReplicaUids.MAILBOX_RECORDS)) {
			aclContainer = IMailboxAclUids.uidForMailbox(desc.owner);
		}
		this.rbacManager = RBACManager.forContext(context).forContainer(aclContainer);
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		DataSource dest = context.getMailboxDataSource(serverUid);
		MapiRuleStore destRuleStore = new MapiRuleStore(dest, ruleStore.folderId);
		try {
			List<MapiRule> toMigrate = ruleStore.all();
			for (MapiRule mr : toMigrate) {
				destRuleStore.store(mr);
				ruleStore.delete(mr.ruleId);
			}
			logger.info("{} rule(s) transferred to {}", toMigrate.size(), serverUid);
		} catch (SQLException se) {
			throw ServerFault.sqlFault(se);
		}
	}

	@Override
	public void updates(MapiRuleChanges changes) {
		rbacManager.check(Verb.Write.name());
		try {
			for (Long ruleId : changes.deleted) {
				ruleStore.delete(ruleId);
			}
			for (MapiRule ruleId : Iterables.concat(changes.updated, changes.created)) {
				ruleStore.store(ruleId);
			}
		} catch (SQLException se) {
			throw ServerFault.sqlFault(se);
		}

	}

	@Override
	public List<MapiRule> all() {
		rbacManager.check(Verb.Read.name());
		try {
			return ruleStore.all();
		} catch (SQLException se) {
			throw ServerFault.sqlFault(se);
		}
	}

}
