package net.bluemind.core.backup.continuous.restore.domains.replication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.RecordKey.Operation;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreDomainType;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.container.api.IRestoreItemCrudSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.Server;

public class RestoreMailboxRecords implements RestoreDomainType {

	private static final Logger logger = LoggerFactory.getLogger(RestoreMailboxRecords.class);

	private final ValueReader<VersionnedItem<MailboxRecord>> recReader = JsonUtils
			.reader(new TypeReference<VersionnedItem<MailboxRecord>>() {
			});

	private final RestoreState state;
	private final IServiceProvider provider;
	private final ItemValue<Domain> domain;
	private final RestoreLogger log;

	public RestoreMailboxRecords(RestoreLogger log, RestoreState state, ItemValue<Domain> domain,
			IServiceProvider provider) {
		this.state = state;
		this.provider = provider;
		this.domain = domain;
		this.log = log;
	}

	public void restore(RecordKey key, String payload) {
		IRestoreItemCrudSupport<MailboxRecord> api = api(key);
		if (Operation.isDelete(key)) {
			delete(key, payload, api);
		} else {
			filterCreateOrUpdate(key, payload, api);
		}
	}

	public String type() {
		return IMailReplicaUids.MAILBOX_RECORDS;
	}

	protected ValueReader<VersionnedItem<MailboxRecord>> reader() {
		return recReader;
	}

	private IRestoreItemCrudSupport<MailboxRecord> api(RecordKey key) {
		String ownerUid = key.owner.split("/")[0];
		logger.info("SCL - ownerUid : {}", ownerUid);
		ItemValue<Mailbox> mbox = state.getMailbox(ownerUid);
		logger.info("SCL - mbox : {}", mbox);
		IDbReplicatedMailboxes apiReplicatedMailboxes = provider.instance(IDbReplicatedMailboxes.class,
				partition(this.domain.uid), mboxRoot(mbox));
		String containerId = key.uid.replace(IMailReplicaUids.MAILBOX_RECORDS_PREFIX, "");
		logger.info("SCL - containerId : {}", containerId);
		ItemValue<MailboxFolder> folder = apiReplicatedMailboxes.getComplete(containerId);
		logger.info("SCL - folder : {}", folder);
		return provider.instance(IDbMailboxRecords.class, folder.uid);
	}

	private void filterCreateOrUpdate(RecordKey key, String payload, IRestoreItemCrudSupport<MailboxRecord> api) {
		VersionnedItem<MailboxRecord> item = reader().read(payload);
		createOrUpdate(api, key, item);
	}

	private void createOrUpdate(IRestoreItemCrudSupport<MailboxRecord> api, RecordKey key,
			VersionnedItem<MailboxRecord> item) {
		logger.info("SCL - RestoreMailboxRecords {}", key);
		boolean exists = exists(api, key, item);

		IDbMessageBodies apiMessageBody = apiMessageBody(key);
		String guid = item.value.messageBody;
		if (apiMessageBody != null && item.value.messageBody != null && !apiMessageBody.exists(guid)) {
			ItemValue<MessageBody> itemMessageBody = new ItemValue<>();
			MessageBody mb = new MessageBody();
			mb.guid = guid;
			mb.created = item.created;
			mb.date = item.created;
			itemMessageBody.value = mb;
			apiMessageBody.restore(itemMessageBody, false);
		}

		if (exists) {
			log.update(type(), key);
			update(api, key, item);
		} else {
			log.create(type(), key);
			create(api, key, item);
		}
	}

	private boolean exists(IRestoreItemCrudSupport<MailboxRecord> api, RecordKey key,
			VersionnedItem<MailboxRecord> item) {
		ItemValue<MailboxRecord> previous = api.getComplete(item.uid);
		if (previous != null && previous.internalId != item.internalId) {
			log.deleteByProduct(type(), key);
			delete(api, key, item.uid);
			return false;
		} else {
			return previous != null;
		}
	}

	private void delete(RecordKey key, String payload, IRestoreItemCrudSupport<MailboxRecord> api) {
		try {
			log.delete(type(), key);
			JsonObject deleteObject = new JsonObject(payload);
			delete(api, key, deleteObject.getString("uid"));
		} catch (ServerFault sf) {
			if (!ErrorCode.NOT_FOUND.equals(sf.getCode())) {
				throw sf;
			}
		}
	}

	private void delete(IRestoreItemCrudSupport<MailboxRecord> api, RecordKey key, String uid) {
		api.delete(uid);
	}

	private void create(IRestoreItemCrudSupport<MailboxRecord> api, RecordKey key, VersionnedItem<MailboxRecord> item) {
		try {
			ItemValue<MailboxRecord> toRestore = map(item, true);
			api.restore(toRestore, true);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
				log.failureIgnored(type(), key, "Item already exists and can't be created, trying to delete."
						+ "This is probably due to an asynchronous item creation. This should be handled on the API side.");
				createOrUpdate(api, key, item);
			}
		}
	}

	private ItemValue<MailboxRecord> map(VersionnedItem<MailboxRecord> item, boolean isCreate) {
		return item;
	}

	private void update(IRestoreItemCrudSupport<MailboxRecord> api, RecordKey key, VersionnedItem<MailboxRecord> item) {
		ItemValue<MailboxRecord> toRestore = map(item, false);
		api.restore(toRestore, false);
	}

	private IDbMessageBodies apiMessageBody(RecordKey key) {
		String ownerUid = key.owner.split("/")[0];
		ItemValue<Mailbox> mbox = state.getMailbox(ownerUid);
		if (mbox == null) {
			log.skip(type(), key, null);
			return null;
		}
		ItemValue<Server> imap = state.getServer(mbox.value.dataLocation);
		CyrusPartition partition = CyrusPartition.forServerAndDomain(imap, domain.uid);
		return provider.instance(IDbMessageBodies.class, partition.name);
	}

	private String mboxRoot(ItemValue<Mailbox> mbox) {
		if (mbox.value.type.sharedNs) {
			return mbox.value.name.replace(".", "^");
		}
		return "user." + mbox.value.name.replace(".", "^");
	}

	private String partition(String domainUid) {
		return domainUid.replace(".", "_");
	}

}
