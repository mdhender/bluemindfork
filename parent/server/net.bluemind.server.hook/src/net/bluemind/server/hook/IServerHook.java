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

package net.bluemind.server.hook;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.server.api.Server;

/**
 * Hook interface for {@link Server} changes (including tagging and domain
 * assignments).
 *
 */
public interface IServerHook {

	void beforeCreate(BmContext context, String uid, Server server) throws ServerFault;

	void beforeUpdate(BmContext context, String uid, Server server, Server previous) throws ServerFault;

	/**
	 * This is called after a server is created in the database.
	 * 
	 * @param context
	 * @param item
	 * @throws ServerFault
	 */
	void onServerCreated(BmContext context, ItemValue<Server> item) throws ServerFault;

	/**
	 * This is called after a server is updated in the database.
	 * 
	 * @param context
	 * @param previousValue
	 * @param value
	 * @throws ServerFault
	 */
	void onServerUpdated(BmContext context, ItemValue<Server> previousValue, Server value) throws ServerFault;

	/**
	 * This is called after a server is deleted in the database.
	 * 
	 * @param context
	 * @param itemValue
	 * @throws ServerFault
	 */
	void onServerDeleted(BmContext context, ItemValue<Server> itemValue) throws ServerFault;

	/**
	 * This is called after a server is tagged in the database (one server
	 * update can trigger multiple onServerTagged events)
	 * 
	 * @param context
	 * @param itemValue
	 * @param tag
	 * @throws ServerFault
	 */
	void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault;

	/**
	 * This is called after a server is untagged in the database.
	 * 
	 * @param context
	 * @param itemValue
	 * @param tag
	 * @throws ServerFault
	 */
	void onServerUntagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault;

	/**
	 * This is called after a server is assigned to a domain for one of its
	 * tags.
	 * 
	 * @param context
	 * @param itemValue
	 * @param domain
	 * @param tag
	 * @throws ServerFault
	 */
	void onServerAssigned(BmContext context, ItemValue<Server> itemValue, ItemValue<Domain> domain, String tag)
			throws ServerFault;

	/**
	 * This is called before an assignment is removed.
	 * 
	 * @param context
	 * @param itemValue
	 * @param domain
	 * @param tag
	 * @throws ServerFault
	 */
	void onServerPreUnassigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> domain, String tag)
			throws ServerFault;

	/**
	 * This is called after an assignment is removed.
	 * 
	 * @param context
	 * @param itemValue
	 * @param domain
	 * @param tag
	 * @throws ServerFault
	 */
	void onServerUnassigned(BmContext context, ItemValue<Server> itemValue, ItemValue<Domain> domain, String tag)
			throws ServerFault;

}
