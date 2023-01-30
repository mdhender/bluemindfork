package net.bluemind.core.backup.continuous.restore.domains.replication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.RecordKey.Operation;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreDomainType;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;

public class RestoreReplicatedMailboxes implements RestoreDomainType {
	private static final Logger logger = LoggerFactory.getLogger(RestoreReplicatedMailboxes.class);
	private static final ValueReader<VersionnedItem<MailboxReplica>> mrReader = JsonUtils
			.reader(new TypeReference<VersionnedItem<MailboxReplica>>() {
			});

	private final RestoreState state;
	private final IServiceProvider target;
	protected final RestoreLogger log;
	protected final ItemValue<Domain> domain;

	public RestoreReplicatedMailboxes(RestoreLogger log, ItemValue<Domain> domain, RestoreState state,
			IServiceProvider target) {
		this.log = log;
		this.domain = domain;
		this.state = state;
		this.target = target;
		logger.debug("init with state {}", this.state);
	}

	public String type() {
		return IMailReplicaUids.REPLICATED_MBOXES;
	}

	protected ValueReader<VersionnedItem<MailboxReplica>> reader() {
		return mrReader;
	}

	private IDbReplicatedMailboxes api(RecordKey key) {
		return target.instance(IDbByContainerReplicatedMailboxes.class, key.uid);
	}

	@Override
	public void restore(RecordKey key, String payload) {
		IDbReplicatedMailboxes api = api(key);
		if (Operation.isDelete(key)) {
			delete(key, payload, api);
		} else {
			filterCreateOrUpdate(key, payload, api);

		}
	}

	private void delete(RecordKey key, String payload, IDbReplicatedMailboxes api) {
		try {
			log.delete(type(), key);
			JsonObject deleteObject = new JsonObject(payload);
			api.delete(deleteObject.getString("uid"));
		} catch (ServerFault sf) {
			if (!ErrorCode.NOT_FOUND.equals(sf.getCode())) {
				throw sf;
			}
		}
	}

	private void filterCreateOrUpdate(RecordKey key, String payload, IDbReplicatedMailboxes api) {
		VersionnedItem<MailboxReplica> item = reader().read(payload);

		if (item.flags.contains(ItemFlag.Deleted)) {
			log.skip(type(), key, payload);
			return;
		}

		boolean exists = api.getComplete(item.uid) != null;
		ItemValue<MailboxReplica> itemValue = map(item);
		MailboxReplica mailboxReplica = itemValue.value;

		if (exists) {
			log.update(type(), key);
			api.update(item.uid, mailboxReplica);
		} else {
			log.create(type(), key);
			api.create(item.uid, mailboxReplica);
		}
	}

	protected ItemValue<MailboxReplica> map(VersionnedItem<MailboxReplica> item) {
		return item;
	}

}