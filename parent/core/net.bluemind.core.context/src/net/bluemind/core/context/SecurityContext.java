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
package net.bluemind.core.context;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SecurityContext {

	private static final CHMInterner interner = new CHMInterner();

	public static final String ROLE_SYSTEM = "systemManagement";

	public static final String ROLE_ADMIN = "admin";

	public static final SecurityContext ANONYMOUS = new SecurityContext(null, "anonymous", "anon",
			Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), null, "en", "internal-anonymous",
			false);

	public static final SecurityContext SYSTEM = new SecurityContext(null, "system", "sys", Collections.emptyList(),
			Arrays.<String>asList(ROLE_SYSTEM), Collections.emptyMap(), "global.virt", "en", "internal-system", false);

	public static final String TOKEN_FAKE_DOMAIN = "token-fake-domain";

	/**
	 * https://shipilev.net/jvm-anatomy-park/10-string-intern/
	 */
	public static class CHMInterner {
		private final Map<String, String> map;

		public CHMInterner() {
			map = new ConcurrentHashMap<>();
		}

		public String intern(String s) {
			if (s == null) {
				return s;
			}
			String exist = map.putIfAbsent(s, s);
			return (exist == null) ? s : exist;
		}
	}

	private final long created;
	private final String sessionId;
	private final String subject;
	private final String subjectDisplayName;
	private final List<String> memberOf;
	private final List<String> roles;
	private final String domainUid;
	private final String lang;
	private final String origin;
	private final Map<String, Set<String>> orgUnitsRoles;
	private List<String> remoteAddresses = Collections.emptyList();
	private final boolean interactive;
	private String ownerSubject;

	/**
	 * Visible for testing
	 * 
	 * @param sessionId
	 * @param subject
	 * @param memberOf
	 * @param roles
	 * @param domainUid
	 */
	public SecurityContext(String sessionId, String subject, String subjectDisplayName, List<String> memberOf,
			List<String> roles, String domainUid) {
		this(sessionId, subject, subjectDisplayName, memberOf, roles, Collections.emptyMap(), domainUid, "en",
				"unknown-origin", false);
	}

	public SecurityContext(String sessionId, String subject, List<String> memberOf, List<String> roles,
			String domainUid) {
		this(sessionId, subject, subject, memberOf, roles, Collections.emptyMap(), domainUid, "en", "unknown-origin",
				false);
	}

	public SecurityContext(String sessionId, String subject, String subjectDisplayName, List<String> memberOf,
			List<String> roles, String domainUid, String lang, String origin) {
		this(sessionId, subject, subjectDisplayName, memberOf, roles, Collections.emptyMap(), domainUid, lang, origin,
				true);
	}

	public SecurityContext(String sessionId, String subject, List<String> memberOf, List<String> roles,
			String domainUid, String lang, String origin) {
		this(sessionId, subject, subject, memberOf, roles, Collections.emptyMap(), domainUid, lang, origin, true);
	}

	public SecurityContext(String sessionId, String subject, String subjectDisplayName, List<String> memberOf,
			List<String> roles, Map<String, Set<String>> rolesByOrgUnit, String domainUid, String lang, String origin) {
		this(sessionId, subject, subjectDisplayName, memberOf, roles, rolesByOrgUnit, domainUid, lang, origin, true);
	}

	public SecurityContext(String sessionId, String subject, List<String> memberOf, List<String> roles,
			Map<String, Set<String>> rolesByOrgUnit, String domainUid, String lang, String origin) {
		this(sessionId, subject, subject, memberOf, roles, rolesByOrgUnit, domainUid, lang, origin, true);
	}

	public SecurityContext(String sessionId, String subject, String subjectDisplayName, List<String> memberOf,
			List<String> roles, Map<String, Set<String>> rolesByOrgUnit, String domainUid, String lang, String origin,
			boolean interactive) {
		this(sessionId, subject, subjectDisplayName, memberOf, roles, rolesByOrgUnit, domainUid, lang, origin,
				interactive, null);
	}

	public SecurityContext(String sessionId, String subject, List<String> memberOf, List<String> roles,
			Map<String, Set<String>> rolesByOrgUnit, String domainUid, String lang, String origin,
			boolean interactive) {
		this(sessionId, subject, subject, memberOf, roles, rolesByOrgUnit, domainUid, lang, origin, interactive, null);
	}

	public SecurityContext(String sessionId, String subject, String subjectDisplayName, List<String> memberOf,
			List<String> roles, Map<String, Set<String>> rolesByOrgUnit, String domainUid, String lang, String origin,
			boolean interactive, String ownerSubject) {
		this(System.currentTimeMillis(), sessionId, subject, subjectDisplayName, memberOf, roles, rolesByOrgUnit,
				domainUid, lang, origin, interactive, null);
	}

	public SecurityContext(long created, String sessionId, String subject, String subjectDisplayName,
			List<String> memberOf, List<String> roles, Map<String, Set<String>> rolesByOrgUnit, String domainUid,
			String lang, String origin, boolean interactive, String ownerSubject) {
		this.created = created;
		this.sessionId = sessionId;
		this.subject = subject;
		this.subjectDisplayName = subjectDisplayName;
		this.memberOf = Collections.unmodifiableList(memberOf);
		// this is visible in gwt, to not change to toList()
		this.roles = roles == null ? Collections.emptyList()
				: Collections.unmodifiableList(roles.stream().map(interner::intern).collect(Collectors.toList()));
		this.orgUnitsRoles = Collections.unmodifiableMap(rolesByOrgUnit);
		this.domainUid = domainUid;
		this.lang = lang;
		this.origin = origin;
		this.interactive = interactive;
		this.ownerSubject = ownerSubject;
	}

	public String getOwnerPrincipal() {
		return Optional.ofNullable(ownerSubject).orElse(subject);
	}

	public void setOwnerPrincipal(String s) {
		this.ownerSubject = s;
	}

	public long getCreated() {
		return created;
	}

	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Returns the user / dirEntry uid
	 * 
	 * @return subject
	 */
	public String getSubject() {
		return subject;
	}

	public String getSubjectDisplayName() {
		return subjectDisplayName;
	}

	public List<String> getMemberOf() {
		return memberOf;
	}

	public List<String> getRoles() {
		return roles;
	}

	/**
	 * Returns the domain uid
	 * 
	 * @return the domain uid
	 */
	public String getContainerUid() {
		return domainUid;
	}

	public String getLang() {
		return lang;
	}

	public String getOrigin() {
		return origin;
	}

	public boolean isDomainGlobal() {
		return roles.contains(ROLE_SYSTEM);
	}

	public boolean fromGlobalVirt() {
		return "global.virt".equals(domainUid);
	}

	public boolean isDomainAdmin(String domainUid) {
		return isDomainGlobal()
				|| (this.domainUid != null && this.domainUid.equals(domainUid) && roles.contains(ROLE_ADMIN));
	}

	public boolean isAdmin() {
		return isDomainGlobal() || roles.contains(ROLE_ADMIN);
	}

	public boolean isAnonymous() {
		return this.subject.equals(ANONYMOUS.subject) && this.domainUid == null;
	}

	public List<String> getRemoteAddresses() {
		return remoteAddresses;
	}

	@Override
	public String toString() {
		return "SecurityContext[sessionId=" + sessionId + ", subject=" + subject + ", memberOf=" + memberOf + ", roles="
				+ roles + ", domainUid=" + domainUid + ", lang=" + lang + "]";
	}

	public final SecurityContext from(List<String> remoteAddresses) {
		return from(remoteAddresses, null);
	}

	public SecurityContext from(List<String> remoteAddresses, String headerOrigin) {
		SecurityContext ret = new SecurityContext(created, sessionId, subject, subjectDisplayName, memberOf, roles,
				orgUnitsRoles, domainUid, lang, bestOrigin(origin, headerOrigin), interactive, ownerSubject);
		ret.remoteAddresses = remoteAddresses;
		return ret;
	}

	private String bestOrigin(String cur, String headerOrigin) {
		if (headerOrigin == null) {
			return cur;
		}
		if (SYSTEM.origin.equals(cur)) {
			return headerOrigin;
		}
		return cur;
	}

	public boolean isInteractive() {
		return interactive;
	}

	public Set<String> getRolesForOrgUnit(Collection<String> path) {
		Set<String> ret = new HashSet<>();

		for (String uid : path) {
			ret.addAll(orgUnitsRoles.getOrDefault(uid, Collections.emptySet()));
		}
		return ret;
	}

	public Map<String, Set<String>> getRolesByOrgUnits() {
		return orgUnitsRoles;
	}

	public void withRolesOnOrgUnit(String ouUid, Set<String> roles) {
		orgUnitsRoles.put(ouUid, roles);
	}

}
