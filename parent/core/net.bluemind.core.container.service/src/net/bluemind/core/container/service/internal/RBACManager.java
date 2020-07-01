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
package net.bluemind.core.container.service.internal;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public class RBACManager {

	private static final Logger logger = LoggerFactory.getLogger(RBACManager.class);
	private BmContext context;
	private String domain;
	private IDirectory directory;
	private DirEntryPermissionResolver dirEntryResolver;
	private ContainerPermissionResolver containerPermissionResolver;
	private DirectPermissionResolver directPermissionResolver;
	private String dirEntryUid;
	private Container container;

	public RBACManager(BmContext context) {
		this.context = context;
		this.directPermissionResolver = new DirectPermissionResolver(context);
	}

	public final boolean can(String... roles) {
		return can(ImmutableSet.copyOf(roles));
	}

	public Set<Permission> resolve() {
		Set<Permission> perms = new HashSet<>();
		if (containerPermissionResolver != null) {
			perms.addAll(containerPermissionResolver.resolve());
		}

		if (dirEntryResolver != null) {
			perms.addAll(dirEntryResolver.resolve());
		}

		perms.addAll(directPermissionResolver.resolve());

		return perms;
	}

	private static final class PermContext {
		Set<Permission> perms = new HashSet<>();
		Set<Permission> checkPerms = new HashSet<>();

	}

	private PermContext buildPermContext(Set<String> roles) {

		PermContext pContext = new PermContext();

		for (String role : roles) {
			pContext.checkPerms.add(new SimplePermission(role));
		}

		for (String role : roles) {
			try {
				Verb verb = Verb.valueOf(role);
				Permission perm = ContainerPermission.asPerm(verb);
				pContext.checkPerms.add(perm);
			} catch (IllegalArgumentException e) {

			}
		}

		if (containerPermissionResolver != null) {
			pContext.perms.addAll(containerPermissionResolver.resolve());
		}

		if (dirEntryResolver != null) {
			pContext.perms.addAll(dirEntryResolver.resolve());
		}
		if (domain != null && dirEntryUid != null) {
			DirEntry entry = directory().findByEntryUid(dirEntryUid);
			if (entry != null) {
				for (String role : roles) {

					Permission perm = DirEntryPermission.create(entry.kind, role);
					pContext.checkPerms.add(perm);
				}
			} else {
				for (String role : roles) {
					Permission perm = DirEntryPermission.create(DirEntry.Kind.DOMAIN, role);
					pContext.checkPerms.add(perm);
				}
			}
		} else if (domain != null) {
			for (String role : roles) {
				Permission perm = DirEntryPermission.create(DirEntry.Kind.DOMAIN, role);
				pContext.checkPerms.add(perm);
			}
		}

		pContext.perms.addAll(directPermissionResolver.resolve());

		return pContext;
	}

	public boolean canAll(Set<String> roles) {
		if (roles.isEmpty()) {
			return true;
		}
		if (context.getSecurityContext().isDomainGlobal()) {
			return true;
		}

		PermContext pContext = buildPermContext(roles);
		boolean canAll = true;
		for (Permission cp : pContext.checkPerms) {
			logger.debug("check perm {} with perms {}", cp, pContext.perms);
			boolean can = false;
			for (Permission p : pContext.perms) {

				if (p.implies(cp)) {
					can = true;
					break;
				}
			}
			canAll = canAll && can;
		}
		return canAll;
	}

	public boolean can(Set<String> roles) {
		if (roles.isEmpty()) {
			return true;
		}
		if (context.getSecurityContext().isDomainGlobal()) {
			return true;
		}

		PermContext pContext = buildPermContext(roles);
		for (Permission cp : pContext.checkPerms) {
			logger.debug("check perm {} with perms {}", cp, pContext.perms);
			for (Permission p : pContext.perms) {

				if (p.implies(cp)) {
					return true;
				}
			}
		}
		return false;
	}

	public void checkNotAnoynmous() throws ServerFault {
		if (context.getSecurityContext().isAnonymous()) {
			throw new ServerFault("not authorized call", ErrorCode.PERMISSION_DENIED);
		}
	}

	public void check(Set<String> roles) throws ServerFault {

		boolean can = can(roles);

		if (!can) {
			if (container != null) {
				throw new ServerFault(String.format("%s@%s Doesnt have role %s on container %s@%s ", //
						context.getSecurityContext().getSubject(), //
						context.getSecurityContext().getContainerUid(), //
						String.join(",", roles), //
						container.uid, container.domainUid), ErrorCode.PERMISSION_DENIED);
			} else if (dirEntryUid != null) {
				throw new ServerFault(String.format("%s@%s Doesnt have role %s on dirEntry %s@%s ", //
						context.getSecurityContext().getSubject(), context.getSecurityContext().getContainerUid(), //
						String.join(",", roles), //
						dirEntryUid, domain), ErrorCode.PERMISSION_DENIED);
			} else if (domain != null) {
				throw new ServerFault(String.format("%s@%s Doesnt have role %s on domain %s ", //
						context.getSecurityContext().getSubject(), context.getSecurityContext().getContainerUid(), //
						String.join(",", roles), domain), ErrorCode.PERMISSION_DENIED);
			} else {
				throw new ServerFault(String.format("%s@%s Doesnt have role %s", //
						context.getSecurityContext().getSubject(), context.getSecurityContext().getContainerUid(), //
						String.join(",", roles)), ErrorCode.PERMISSION_DENIED);
			}
		}
	}

	public final void check(String... roles) throws ServerFault {
		check(ImmutableSet.<String>builder().add(roles).build());
	}

	public RBACManager forContainer(String uid) throws ServerFault {
		Container container = null;
		try {

			container = new ContainerStore(context, DataSourceRouter.get(context, uid), context.getSecurityContext())
					.get(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (container == null) {
			throw new ServerFault("container " + uid + " not found", ErrorCode.NOT_FOUND);
		}

		return forContainer(container);
	}

	public RBACManager forContainer(Container container) throws ServerFault {

		RBACManager ret = new RBACManager(context);
		ret.containerPermissionResolver = new ContainerPermissionResolver(context, container);
		ret.container = container;
		if (container.domainUid != null) {
			ret.domain = container.domainUid;
			ret.dirEntryUid = container.owner;
			ret.dirEntryResolver = new DirEntryPermissionResolver(context, ret.domain, ret.dirEntryUid, null);
		} else {
			logger.warn("container {} domain uid is null ", container.uid);
			ret.domain = null;
			ret.dirEntryUid = null;
			ret.dirEntryResolver = null;
		}
		return ret;
	}

	public RBACManager forEntry(String uid) throws ServerFault {
		RBACManager ret = new RBACManager(context);
		ret.containerPermissionResolver = null;
		ret.container = null;
		ret.domain = domain;
		ret.dirEntryUid = uid;
		ret.dirEntryResolver = new DirEntryPermissionResolver(context, ret.domain, ret.dirEntryUid, null);

		return ret;
	}

	public RBACManager forDomain(String domainUid) {
		RBACManager ret = new RBACManager(context);
		ret.container = null;
		ret.containerPermissionResolver = null;
		ret.domain = domainUid;
		ret.dirEntryUid = null;
		ret.dirEntryResolver = new DirEntryPermissionResolver(context, ret.domain, null, null);
		return ret;
	}

	public RBACManager forOrgUnit(String orgUnitUid) {
		if (orgUnitUid == null) {
			return this;
		}
		RBACManager ret = new RBACManager(context);

		ret.containerPermissionResolver = null;
		ret.container = null;
		ret.domain = domain;
		ret.dirEntryUid = null;
		ret.dirEntryResolver = new DirEntryPermissionResolver(context, ret.domain, null, orgUnitUid);

		return ret;
	}

	public Set<String> roles() {
		return resolve().stream().map(perm -> perm.asRole()).collect(Collectors.toSet());
	}

	public List<String> directRoles() {
		return context.getSecurityContext().getRoles();
	}

	private static final class SystemRBACManager extends RBACManager {

		public SystemRBACManager(BmContext context) {
			super(context);
		}

		@Override
		public boolean canAll(Set<String> roles) {
			return true;
		}

		@Override
		public boolean can(Set<String> roles) {
			return true;
		}

		@Override
		public void checkNotAnoynmous() throws ServerFault {
			// ok
		}

		@Override
		public void check(Set<String> roles) throws ServerFault {
			// ok
		}

		@Override
		public RBACManager forContainer(String rbacUid) throws ServerFault {
			return this;
		}

		@Override
		public RBACManager forContainer(Container container) throws ServerFault {
			return this;
		}

		@Override
		public RBACManager forEntry(String uid) throws ServerFault {
			return this;
		}

		@Override
		public RBACManager forDomain(String domainUid) {
			return this;
		}

		@Override
		public RBACManager forOrgUnit(String orgUnitUid) {
			return this;
		}

		@Override
		public Set<Permission> resolve() {
			return Sets.newHashSet(ContainerPermission.ALL);
		}

	}

	public static RBACManager forContext(BmContext context) {
		if (context.getSecurityContext().isDomainGlobal()) {
			return new SystemRBACManager(context);
		} else {
			return new RBACManager(context);
		}
	}

	public static RBACManager forSecurityContext(SecurityContext tok) {
		return forContext(ServerSideServiceProvider.getProvider(tok).getContext());
	}

	private IDirectory directory() throws ServerFault {
		if (domain == null) {
			throw new ServerFault("domain is not defined");
		}
		if (directory == null) {
			directory = context.su().provider().instance(IDirectory.class, domain);
		}
		return directory;
	}

}
