package net.bluemind.core.backup.continuous.restore.domains.crud;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.Server;

public class RestoreMessageBody extends CrudRestore<MessageBody> {

	private final IServiceProvider target;
	private final RestoreState state;

	private static final ValueReader<VersionnedItem<MessageBody>> reader = JsonUtils
			.reader(new TypeReference<VersionnedItem<MessageBody>>() {
			});

	public RestoreMessageBody(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target,
			RestoreState state) {
		super(log, domain);
		this.target = target;
		this.state = state;
	}

	@Override
	public String type() {
		return IMailReplicaUids.REPAIR_MESSAGE_BODIES;
	}

	@Override
	protected ValueReader<VersionnedItem<MessageBody>> reader() {
		return reader;
	}

	@Override
	protected IDbMessageBodies api(ItemValue<Domain> domain, RecordKey key) {
		String ownerUid = key.owner.split("/")[0];
		ItemValue<Mailbox> mbox = state.getMailbox(ownerUid);
		if (mbox == null) {
			log.skip(type(), key, null);
			return null;
		}
		ItemValue<Server> imap = state.getServer(mbox.value.dataLocation);
		CyrusPartition partition = CyrusPartition.forServerAndDomain(imap, domain.uid);
		return target.instance(IDbMessageBodies.class, partition.name);
	}

}
