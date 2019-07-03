package net.bluemind.group.service.internal;

import java.util.List;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.group.api.Group;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class GroupValidator {
	private IServer serverService;
	private String domainUid;

	public GroupValidator(IServer serverService, String domainUid) {
		this.serverService = serverService;
		this.domainUid = domainUid;
	}

	public void validate(String uid, String extId, Group group) throws ServerFault {
		ParametersValidator.notNullAndNotEmpty(uid);
		ParametersValidator.nullOrNotEmpty(extId);
		ParametersValidator.notNull(group);

		if (group.name == null || group.name.trim().isEmpty()) {
			throw new ServerFault("The group name must be filled in", ErrorCode.INVALID_PARAMETER);
		}

		if (!group.mailArchived) {
			return;
		}

		if (group.dataLocation == null || group.dataLocation.trim().isEmpty()) {
			throw new ServerFault("Undefined data location server for group: " + group.name,
					ErrorCode.INVALID_PARAMETER);
		}

		ItemValue<Server> server = serverService.getComplete(group.dataLocation);
		if (server == null) {
			throw new ServerFault("Server doesn't exist: " + group.dataLocation, ErrorCode.INVALID_PARAMETER);
		}

		List<Assignment> assignments = serverService.getAssignments(domainUid);

		boolean assigned = false;
		boolean taggedAsImap = false;
		for (Assignment assignment : assignments) {
			if (assignment.serverUid.equals(group.dataLocation)) {
				assigned = true;

				if (assignment.tag.equals("mail/imap")) {
					taggedAsImap = true;
				}
			}
		}

		if (!assigned) {
			throw new ServerFault("Server uid: " + group.dataLocation + " not assigned to domain: " + domainUid,
					ErrorCode.INVALID_PARAMETER);
		}

		if (!taggedAsImap) {
			throw new ServerFault("Server uid: " + group.dataLocation + " not taggued as mail/imap",
					ErrorCode.INVALID_PARAMETER);
		}
	}

	public void validate(String uid, Group group) throws ServerFault {
		validate(uid, null, group);
	}
}
