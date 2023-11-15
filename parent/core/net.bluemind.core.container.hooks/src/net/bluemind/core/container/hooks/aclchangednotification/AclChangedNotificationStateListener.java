package net.bluemind.core.container.hooks.aclchangednotification;

import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;

public class AclChangedNotificationStateListener implements IStateListener {
	@Override
	public void stateChanged(SystemState newState) {
		if (newState == SystemState.CORE_STATE_STOPPING) {
			VertxPlatform.eventBus().publish(AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_TEARDOWN_BUS_ADDRESS, null);
		}
	}
}
