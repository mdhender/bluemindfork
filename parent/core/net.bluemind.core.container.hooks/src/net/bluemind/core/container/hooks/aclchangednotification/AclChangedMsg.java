package net.bluemind.core.container.hooks.aclchangednotification;

import java.util.List;

import net.bluemind.core.container.model.acl.AccessControlEntry;

public record AclChangedMsg(String sourceUserId, String domainUid, String containerUid, String containerName,
		String containerType, boolean defaultContainer, List<AccessControlEntry> diff) {
}
