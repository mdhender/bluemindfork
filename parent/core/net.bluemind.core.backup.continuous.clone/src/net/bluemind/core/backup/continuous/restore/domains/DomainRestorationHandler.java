package net.bluemind.core.backup.continuous.restore.domains;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.ISeppukuAckListener;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreCalendarView;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreDeferredAction;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreDevice;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreMailboxIdentity;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreMailflow;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreOwnerSubscriptions;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreResourceType;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreTags;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreUserAccounts;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreUserMailIdentities;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreVCard;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreVEventSeries;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreVNote;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreVTodo;
import net.bluemind.core.backup.continuous.restore.domains.crud.RestoreWebAppData;
import net.bluemind.core.backup.continuous.restore.domains.crud.directories.RestoreDirectories;
import net.bluemind.core.backup.continuous.restore.domains.replication.RestoreMailboxRecords;
import net.bluemind.core.backup.continuous.restore.domains.replication.RestoreMessageBody;
import net.bluemind.core.backup.continuous.restore.domains.replication.RestoreMessageBodyESSource;
import net.bluemind.core.backup.continuous.restore.domains.replication.RestoreReplicatedMailboxes;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.Domain;

public class DomainRestorationHandler implements Handler<DataElement> {

	private final RestoreLogger log;
	private final Map<String, RestoreDomainType> restoresByType;
	private final Set<String> skip;

	public DomainRestorationHandler(IServerTaskMonitor monitor, Set<String> skip, ItemValue<Domain> domain,
			IServiceProvider target, ISeppukuAckListener byeAck, RestoreState state) {
		this.log = new RestoreLogger(monitor);
		this.skip = skip;
		this.restoresByType = Arrays.asList(//
				new RestoreMailboxRecords(log, state, domain, target), //
				new RestoreMessageBody(log, domain, target, state), //
				new RestoreMessageBodyESSource(log), //
				new RestoreDirectories(log, domain, target, byeAck, state), //
				new RestoreReplicatedMailboxes(log, domain, state, target), //
				new RestoreMapiArtifacts(log, domain, target), //
				new RestoreFlatHierarchy(log, domain, target), //
				new RestoreVCard(log, domain, target), //
				new RestoreVEventSeries(log, domain, target), //
				new RestoreDeferredAction(log, domain, target), //
				new RestoreVTodo(log, domain, target), //
				new RestoreVNote(log, domain, target), //
				new RestoreMembership(log, domain, target), //
				new RestoreRoles(log, domain, target), //
				new RestoreOrgUnitAdminRoles(log, domain, target), //
				new RestoreResourceType(log, domain, target), //
				new RestoreMailFilter(log, domain, target), //
				new RestoreContainerMetadata(log, target), //
				new RestoreOwnerSubscriptions(log, domain, target), //
				new RestoreTags(log, domain, target), //
				new RestoreDevice(log, domain, target), //
				new RestoreMailflow(log, domain, target), //
				new RestoreUserAccounts(log, domain, target), //
				new RestoreMailboxIdentity(log, domain, target), //
				new RestoreUserMailIdentities(log, domain, target), //
				new RestoreWebAppData(log, domain, target), //
				new RestoreCalendarView(log, domain, target)) // //
				.stream().collect(Collectors.toMap(RestoreDomainType::type, Function.identity()));
	}

	@Override
	public void handle(DataElement event) {
		RestoreDomainType restore = restoresByType.get(event.key.type);
		String payload = new String(event.payload);
		if (restore != null && !skip.contains(event.key.type)) {
			try {
				restore.restore(event.key, payload);
			} catch (Throwable e) {
				log.monitor().log("[{}:{}] Failure processing type {}", event.part, event.offset, event.key.type);
				log.failure(restore.type(), event.key, payload, e);
				throw e;
			}
		} else {
			log.skip(event.key.type, event.key, payload);
		}
	}

}
