package net.bluemind.core.container.hooks.aclchangednotification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.hooks.aclchangednotification.AclWithState.AclStatus;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class AclChangedNotificationHook implements IAclHook {

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		previous = AccessControlEntry.compact(previous);
		current = AccessControlEntry.compact(current);

		List<AclWithState> diff = prepareAclDiff(previous, current);
		if (!diff.isEmpty()) {
			AclChangedMsg aclChangeMsg = new AclChangedMsg(context.getSecurityContext().getSubject(),
					context.getSecurityContext().getContainerUid(), container.uid, container.name, container.type,
					container.ownerDisplayname, diff,
					context.getSecurityContext().getSubject().equals(container.owner));
			VertxPlatform.eventBus().publish(
					AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS,
					new LocalJsonObject<>(aclChangeMsg));
		}
	}

	private static List<AclWithState> prepareAclDiff(List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		Set<AclWithState> aclsWithStatus = new HashSet<>();

		List<AclWithState> newVerbs = current.stream().filter(e -> !previous.contains(e))
				.map(ace -> new AclWithState(ace, AclStatus.ADDED)).toList();
		List<AclWithState> oldVerbs = previous.stream().filter(e -> !current.contains(e))
				.map(ace -> new AclWithState(ace, AclStatus.REMOVED)).toList();

		aclsWithStatus.addAll(oldVerbs);
		aclsWithStatus.addAll(newVerbs);

		return aclsWithStatus.stream().toList();

	}
}
