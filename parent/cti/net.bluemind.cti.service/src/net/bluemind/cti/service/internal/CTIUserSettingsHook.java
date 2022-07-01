package net.bluemind.cti.service.internal;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.cti.service.CTIDeferredAction;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.deferredaction.api.IInternalDeferredAction;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.hook.settings.IUserSettingsHook;

public class CTIUserSettingsHook implements IUserSettingsHook {

	@Override
	public void onSettingsUpdate(String containerUid, String userUid) {
		IInternalDeferredAction deferredActionService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInternalDeferredAction.class, IDeferredActionContainerUids.uidForDomain(containerUid));
		Map<String, String> settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, containerUid).get(userUid);

		List<ItemValue<DeferredAction>> storedTriggers = deferredActionService
				.getByReference(CTIDeferredAction.reference(userUid));
		if (hasCalPresenceChanged(settings, storedTriggers)) {
			storedTriggers.forEach(action -> deferredActionService.delete(action.uid));
			if (isCalPresenceSet(settings)) {
				DeferredAction deferredAction = new DeferredAction();
				deferredAction.executionDate = new Date(Instant.now().toEpochMilli());
				deferredAction.actionId = CTIDeferredAction.ACTION_ID;
				deferredAction.reference = CTIDeferredAction.reference(userUid);
				deferredActionService.create(deferredAction);
			}
		}

	}

	private boolean hasCalPresenceChanged(Map<String, String> settings,
			List<ItemValue<DeferredAction>> storedTriggers) {
		return isCalPresenceSet(settings) == storedTriggers.isEmpty();
	}

	private boolean isCalPresenceSet(Map<String, String> settings) {
		return !"false".equals(settings.get("cal_set_phone_presence"));
	}

}
