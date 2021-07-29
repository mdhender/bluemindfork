package net.bluemind.core.backup.continuous.restore.domains;

import java.util.ArrayList;

import io.vertx.core.Handler;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.IClonePhaseObserver;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.Domain;
import net.bluemind.sds.store.ISdsSyncStore;

public class DomainRestorationHandler implements Handler<DataElement> {

	private final RestoreMailboxRecords mailboxItemRestoration;
	private final RestoreDirectories directoriesRestoration;
	private final RestoreReplicatedMailboxes replicatedMailboxesRestoration;
	private final RestoreMembership membershipRestoration;

	public DomainRestorationHandler(IServerTaskMonitor monitor, ItemValue<Domain> domain, IServiceProvider target,
			ArrayList<IClonePhaseObserver> observers, ISdsSyncStore sdsStore, RestoreState state) {
		mailboxItemRestoration = new RestoreMailboxRecords(monitor, sdsStore, state);
		directoriesRestoration = new RestoreDirectories(monitor, target, observers, state);
		replicatedMailboxesRestoration = new RestoreReplicatedMailboxes(monitor, domain, state);
		membershipRestoration = new RestoreMembership(monitor, target);
	}

	@Override
	public void handle(DataElement event) {
		switch (event.key.type) {
		case "dir":
			directoriesRestoration.restore(event);
			break;
		case "memberships":
			membershipRestoration.restore(event);
			break;
		case IMailReplicaUids.MAILBOX_RECORDS:
			mailboxItemRestoration.restore(event);
			break;
		case "replicated_mailboxes":
			replicatedMailboxesRestoration.restore(event);
			break;
		}
	}

}
