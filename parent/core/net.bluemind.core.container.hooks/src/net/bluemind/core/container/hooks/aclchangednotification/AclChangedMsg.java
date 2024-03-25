package net.bluemind.core.container.hooks.aclchangednotification;

import java.util.List;
import java.util.stream.Collectors;

public record AclChangedMsg(String sourceUserId, String domainUid, String containerUid, String containerName,
		String containerType, String containerOwnerDisplayname, List<AclWithStatus> changes, boolean isItsOwnContainer) {

	@Override
	public String toString() {
		return "AclChangedMsg [domainUid=" + domainUid + ", sourceUserId=" + sourceUserId + ", containerUid="
				+ containerUid + ", containerName=" + containerName + ", containerType=" + containerType
				+ ", containerOwnerDisplayname=" + containerOwnerDisplayname + ", isItsOwnContainer="
				+ isItsOwnContainer + ", acls=(" + aclToString() + ")]";
	}

	private String aclToString() {
		return changes.stream().map(ace -> ace.entry().toString() + ": " + ace.status()).collect(Collectors.joining(", "));
	}
}
