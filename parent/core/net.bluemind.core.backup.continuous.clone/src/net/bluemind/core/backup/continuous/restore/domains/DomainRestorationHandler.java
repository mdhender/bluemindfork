package net.bluemind.core.backup.continuous.restore.domains;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.IClonePhaseObserver;
import net.bluemind.core.backup.continuous.restore.ISeppukuAckListener;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.Domain;
import net.bluemind.sds.store.ISdsSyncStore;

public class DomainRestorationHandler implements Handler<DataElement> {
	private static final Logger logger = LoggerFactory.getLogger(DomainRestorationHandler.class);

	private final Map<String, RestoreDomainType> restoresByType;

	public DomainRestorationHandler(IServerTaskMonitor monitor, ItemValue<Domain> domain, IServiceProvider target,
			List<IClonePhaseObserver> observers, ISdsSyncStore sdsStore, ISeppukuAckListener byeAck,
			RestoreState state) {
		this.restoresByType = Arrays.asList(//
				new RestoreMailboxRecords(monitor, sdsStore, state), //
				new RestoreDirectories(monitor, target, observers, byeAck, state), //
				new RestoreReplicatedMailboxes(monitor, domain, state), //
				new RestoreMapiArtifacts(monitor, domain, target), //
				new RestoreFlatHierarchy(monitor, domain, target), //
				new RestoreVCard(monitor, target), //
				new RestoreVEventSeries(monitor, target), //
				new RestoreVTodo(monitor, target), //
				new RestoreMembership(monitor, domain, target), //
				new RestoreRoles(monitor, domain, target), //
				new RestoreOrgUnitAdminRoles(monitor, domain, target), //
				new RestoreResourceType(monitor, domain, target), //
				new RestoreMailFilter(monitor, domain, target), //
				new RestoreContainerMetadata(monitor, target), //
				new RestoreOwnerSubscriptions(monitor, domain, target)) //
				.stream().collect(Collectors.toMap(RestoreDomainType::type, Function.identity()));
	}

	@Override
	public void handle(DataElement event) {
		RestoreDomainType restore = restoresByType.get(event.key.type);
		// logger.info("Restoring {} with {}", event.key.type, restore);
		if (restore != null) {
			System.err.println("Restore " + event.key.type + " " + event.key.id);
			restore.restore(event);
		} else {
			logger.warn("Skip {}", event.key.type);
		}
	}

}
