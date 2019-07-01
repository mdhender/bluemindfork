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
package net.bluemind.core.rest;

import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import net.bluemind.core.context.SecurityContext;

public interface BmContext {

	public SecurityContext getSecurityContext();

	public DataSource getDataSource();

	public DataSource getMailboxDataSource(String datalocation);

	public List<DataSource> getAllMailboxDataSource();

	public IServiceProvider getServiceProvider();

	public IServiceProvider provider();

	public BmContext su();

	public BmContext su(String userUid, String domainUid);

	public BmContext su(String sid, String userUid, String domainUid);

	public BmContext withRoles(Set<String> roles);
}
