/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.core.container.service.acl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public class MailshareAclSanitize {

	private final ContainerAcl previousAcl;
    private final ContainerAcl currentAcl;

	public MailshareAclSanitize(ContainerAcl previousAcl, ContainerAcl currentAcl) {
		this.previousAcl = previousAcl;
		this.currentAcl = currentAcl;
	}

	public void sanitize() {
		Map<String, List<Verb>> previousMap = aclAsMap(previousAcl.acl());
		Map<String, List<Verb>> currentMap = aclAsMap(currentAcl.acl());

		removeSendAs(previousMap, currentMap);
		addSendAs(previousMap, currentMap);
	}

	private static Map<String, List<Verb>> aclAsMap(Set<AccessControlEntry> acls) {
		return acls.stream().collect(Collectors.groupingBy(AccessControlEntry::getSubject,
				Collectors.mapping(AccessControlEntry::getVerb, Collectors.toList())));
	}

	private void addSendAs(Map<String, List<Verb>> previousMap, Map<String, List<Verb>> currentMap) {
		Set<String> previousWithWrite = containsVerb(previousMap, Verb.Write);
		Set<String> currentWithWrite = containsVerb(currentMap, Verb.Write);

		currentWithWrite.removeAll(previousWithWrite);
		if (currentWithWrite.isEmpty()) {
			return;
		}

		Set<AccessControlEntry> newSendAsAcls = currentAcl.acl().stream().filter(
				acl -> acl.verb != Verb.SendAs && currentWithWrite.stream().anyMatch(s -> acl.subject.equals(s)))
				.map(acl -> AccessControlEntry.create(acl.subject, Verb.SendAs)).collect(Collectors.toSet());

		currentAcl.acl().addAll(newSendAsAcls);
	}

	private void removeSendAs(Map<String, List<Verb>> previousMap, Map<String, List<Verb>> currentMap) {
		Set<String> previousWithWrite = containsVerb(previousMap, Verb.Write);
		Set<String> currentWithWrite = containsVerb(currentMap, Verb.Write);

		previousWithWrite.removeAll(currentWithWrite);
		if (previousWithWrite.isEmpty()) {
			return;
		}

		currentAcl.acl().removeIf(
				acl -> acl.verb == Verb.SendAs && previousWithWrite.stream().anyMatch(s -> acl.subject.equals(s)));
	}

	static boolean isMailshare(BmContext context, String domainUid, String owner) {
		DirEntry entryByUid = context.getServiceProvider().instance(IDirectory.class, domainUid).findByEntryUid(owner);
		return entryByUid != null && entryByUid.kind == Kind.MAILSHARE;
	}

	private static Set<String> containsVerb(Map<String, List<Verb>> acls, Verb verb) {
		return acls.entrySet().stream() //
				.filter(entry -> entry.getValue().stream() //
						.anyMatch(v -> v.can(verb)))
				.map(Entry::getKey).collect(Collectors.toSet());
	}

}
