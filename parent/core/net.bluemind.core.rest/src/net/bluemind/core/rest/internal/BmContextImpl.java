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
package net.bluemind.core.rest.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;

public class BmContextImpl implements BmContext {
	private SecurityContext securityContext;
	private DataSource dataSource;
	private Map<String, DataSource> mailboxDataSource;

	public BmContextImpl(SecurityContext securityContext, DataSource dataSource,
			Map<String, DataSource> mailboxDataSource) {
		this.securityContext = securityContext;
		this.dataSource = dataSource;
		this.mailboxDataSource = mailboxDataSource;
	}

	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	@Override
	public IServiceProvider getServiceProvider() {
		return ServerSideServiceProvider.getProvider(this);
	}

	@Override
	public IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(this);
	}

	@Override
	public BmContext su() {
		return new BmContextImpl(SecurityContext.SYSTEM, dataSource, mailboxDataSource);
	}

	@Override
	public BmContext su(String userUid, String domainUid) {
		return su(null, userUid, domainUid);
	}

	@Override
	public BmContext su(String sid, String userUid, String domainUid) {
		SecurityContext sec = new SecurityContext(sid, userUid, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyMap(), domainUid, "en", "BmContext.su", false);
		if (sid != null) {
			Sessions.get().put(sid, sec);
		}
		return new BmContextImpl(sec, dataSource, mailboxDataSource);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(BmContextImpl.class)//
				.add("sec", securityContext)//
				.toString();
	}

	@Override
	public BmContext withRoles(Set<String> roles) {

		List<String> r = new ArrayList<>(
				ImmutableSet.<String>builder().addAll(roles).addAll(securityContext.getRoles()).build());

		SecurityContext sc = new SecurityContext(securityContext.getSessionId(), securityContext.getSubject(),
				securityContext.getMemberOf(), r, securityContext.getRolesByOrgUnits(),
				securityContext.getContainerUid(), securityContext.getLang(), securityContext.getOrigin(),
				securityContext.isInteractive());
		return new BmContextImpl(sc, dataSource, mailboxDataSource);
	}

	@Override
	public DataSource getMailboxDataSource(String datalocation) {
		return mailboxDataSource.get(datalocation);
	}

	@Override
	public List<DataSource> getAllMailboxDataSource() {
		List<DataSource> ret = new ArrayList<DataSource>();
		ret.addAll(mailboxDataSource.values());
		return ret;
	}

}
