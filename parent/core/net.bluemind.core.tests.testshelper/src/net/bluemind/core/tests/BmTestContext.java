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
package net.bluemind.core.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.role.service.IInternalRoles;

public class BmTestContext implements BmContext {

	private SecurityContext securityContext;
	private IServiceProvider provider;
	private DataSource dataSource;

	public BmTestContext(SecurityContext sc) {
		this(sc, (IServiceProvider) null);
		provider = ServerSideServiceProvider.getProvider(this);
	}

	public BmTestContext(SecurityContext sc, IServiceProvider provider) {
		this.securityContext = sc;
		this.provider = provider;
	}

	public String dataSourceLocation(DataSource ds) {
		if (ds == dataSource) {
			return "dir";
		}
		return ServerSideServiceProvider.mailboxDataSource.entrySet().stream().filter(e -> e.getValue() == ds)
				.map(Entry::getKey).findAny().orElse(null);
	}

	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public DataSource getDataSource() {
		if (dataSource != null) {
			return dataSource;
		} else {
			return JdbcActivator.getInstance().getDataSource();
		}
	}

	@Override
	public IServiceProvider getServiceProvider() {
		return provider;
	}

	@Override
	public IServiceProvider provider() {
		return provider;
	}

	@Override
	public BmContext su() {
		return new BmTestContext(SecurityContext.SYSTEM);
	}

	@Override
	public BmContext su(String userUid, String domainUid) {
		return su(null, userUid, domainUid);
	}

	@Override
	public BmContext su(String sid, String userUid, String domainUid) {
		return new BmTestContext(
				new SecurityContext(sid, userUid, Arrays.<String>asList(), Arrays.<String>asList(), domainUid));
	}

	@Override
	public BmContext withRoles(Set<String> roles) {

		List<String> r = new ArrayList<>(
				ImmutableSet.<String>builder().addAll(roles).addAll(securityContext.getRoles()).build());

		SecurityContext sc = new SecurityContext(securityContext.getSessionId(), securityContext.getSubject(),
				securityContext.getMemberOf(), r, securityContext.getRolesByOrgUnits(),
				securityContext.getContainerUid(), securityContext.getLang(), securityContext.getOrigin());
		return new BmTestContext(sc);
	}

	public BmTestContext withRoles(String... roles) {

		List<String> r = new ArrayList<>(
				ImmutableSet.<String>builder().add(roles).addAll(securityContext.getRoles()).build());

		SecurityContext sc = new SecurityContext(securityContext.getSessionId(), securityContext.getSubject(),
				securityContext.getMemberOf(), r, securityContext.getRolesByOrgUnits(),
				securityContext.getContainerUid(), securityContext.getLang(), securityContext.getOrigin());
		return new BmTestContext(sc);
	}

	public BmTestContext withRolesOnOrgUnit(String ouUid, String... roles) {
		Map<String, Set<String>> rolesBy = new HashMap<>(securityContext.getRolesByOrgUnits());
		rolesBy.put(ouUid, ImmutableSet.<String>builder().add(roles).build());
		SecurityContext sc = new SecurityContext(securityContext.getSessionId(), securityContext.getSubject(),
				securityContext.getMemberOf(), securityContext.getRoles(), rolesBy, securityContext.getContainerUid(),
				securityContext.getLang(), securityContext.getOrigin());
		return new BmTestContext(sc);
	}

	public static BmTestContext contextWithSession(String sid, String subject, String domainUid, String... roles)
			throws ServerFault {
		IInternalRoles roleService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInternalRoles.class);
		SecurityContext sc = new SecurityContext(sid, subject, //
				Arrays.<String>asList(), // members
				new ArrayList<String>(roleService.resolve(new HashSet<String>(Arrays.asList(roles)))), domainUid);
		Sessions.get().put(sid, sc);
		return new BmTestContext(sc);
	}

	public BmTestContext session(String sid) throws ServerFault {

		SecurityContext sc = new SecurityContext(sid, securityContext.getSubject(), securityContext.getMemberOf(),
				securityContext.getRoles(), securityContext.getRolesByOrgUnits(), securityContext.getContainerUid(),
				securityContext.getLang(), securityContext.getOrigin());
		Sessions.get().put(sid, sc);
		return new BmTestContext(sc);
	}

	public static BmTestContext context(String subject, String domainUid, String... roles) throws ServerFault {
		IInternalRoles roleService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInternalRoles.class);

		SecurityContext sc = new SecurityContext(null, subject, //
				Arrays.<String>asList(), // members
				new ArrayList<String>(roleService.resolve(new HashSet<String>(Arrays.asList(roles)))), domainUid);
		return new BmTestContext(sc);
	}

	public BmTestContext withGroup(String... groups) {
		return new BmTestContext(new SecurityContext(securityContext.getSessionId(), securityContext.getSubject(),
				Arrays.asList(groups), securityContext.getRoles(), securityContext.getRolesByOrgUnits(),
				securityContext.getContainerUid(), securityContext.getLang(), securityContext.getOrigin(),
				securityContext.isInteractive()));

	}

	@Override
	public DataSource getMailboxDataSource(String datalocation) {
		return ServerSideServiceProvider.mailboxDataSource.get(datalocation);
	}

	@Override
	public List<DataSource> getAllMailboxDataSource() {
		List<DataSource> ret = new ArrayList<>();
		ret.addAll(ServerSideServiceProvider.mailboxDataSource.values());
		return ret;
	}
}
