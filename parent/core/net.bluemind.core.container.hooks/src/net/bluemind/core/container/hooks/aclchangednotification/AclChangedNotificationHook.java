package net.bluemind.core.container.hooks.aclchangednotification;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.hooks.aclchangednotification.AclDiff.AclStatus;
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

		List<AclDiff> diff = prepareAclDiff(context, previous, current);

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

	private static List<AclDiff> prepareAclDiff(BmContext context, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {

		List<AccessControlEntry> newVerbs = current.stream()
				.filter(e -> !previous.contains(e) && !e.subject.equals(context.getSecurityContext().getSubject()))
				.toList();
		List<AccessControlEntry> oldVerbs = previous.stream()
				.filter(e -> !current.contains(e) && !e.subject.equals(context.getSecurityContext().getSubject()))
				.toList();

		List<AclDiff> diffResult = new ArrayList<AclDiff>();

		for (AccessControlEntry oldStatus : oldVerbs) {
			// search same subject in new
			newVerbs.stream().filter(v -> v.subject.equals(oldStatus.subject)).findFirst()
					.ifPresent(v -> diffResult.add(AclDiff.createAclDiffForUpdate(oldStatus, v)));
		}

		for (AccessControlEntry newStatus : newVerbs) {
			// search same subject in old
			oldVerbs.stream().filter(v -> v.subject.equals(newStatus.subject)).findFirst()
					.ifPresent(v -> diffResult.add(AclDiff.createAclDiffForUpdate(v, newStatus)));
		}

		if (newVerbs.isEmpty()) {
			oldVerbs.forEach(aws -> diffResult.add(AclDiff.createAclDiff(aws, AclStatus.REMOVED)));
		} else {
			oldVerbs.stream().filter(s -> !newVerbs.stream().map(v -> v.subject).toList().contains(s.subject))
					.map(aws -> diffResult.add(AclDiff.createAclDiff(aws, AclStatus.REMOVED)));
		}

		if (oldVerbs.isEmpty()) {
			newVerbs.forEach(aws -> diffResult.add(AclDiff.createAclDiff(aws, AclStatus.ADDED)));
		} else {
			newVerbs.stream().filter(s -> !oldVerbs.stream().map(v -> v.subject).toList().contains(s.subject))
					.map(aws -> diffResult.add(AclDiff.createAclDiff(aws, AclStatus.ADDED)));
		}

		return diffResult;
	}
}
