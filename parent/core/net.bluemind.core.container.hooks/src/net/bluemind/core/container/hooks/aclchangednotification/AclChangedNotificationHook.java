package net.bluemind.core.container.hooks.aclchangednotification;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class AclChangedNotificationHook implements IAclHook {
	private static final Logger logger = LoggerFactory.getLogger(AclChangedNotificationHook.class);
	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {
		List<AccessControlEntry> diff = new ArrayList<>(current);
		diff.removeAll(previous);
		if (!diff.isEmpty()) {
			AclChangedMsg aclChangeMsg = new AclChangedMsg(context.getSecurityContext().getSubject(),
					context.getSecurityContext().getContainerUid(), container.uid, container.name, container.type,
					container.defaultContainer, diff);
			VertxPlatform.eventBus().publish(
					AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS,
					new LocalJsonObject<>(aclChangeMsg));
		}
	}

}
