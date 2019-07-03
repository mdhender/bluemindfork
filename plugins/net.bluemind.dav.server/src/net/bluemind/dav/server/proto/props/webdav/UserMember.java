package net.bluemind.dav.server.proto.props.webdav;

import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.directory.api.DirEntry;

public class UserMember {

	private AccessControlEntry acl;
	private DirEntry dir;

	public UserMember(AccessControlEntry acl, DirEntry dir) {
		this.acl = acl;
		this.dir = dir;
	}

	public AccessControlEntry getAcl() {
		return acl;
	}

	public DirEntry getUserUid() {
		return dir;
	}

}
