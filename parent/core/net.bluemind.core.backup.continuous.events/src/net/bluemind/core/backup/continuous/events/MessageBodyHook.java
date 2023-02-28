package net.bluemind.core.backup.continuous.events;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Suppliers;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.hook.IMessageBodyHook;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.IDirectory;

public class MessageBodyHook implements IMessageBodyHook, ContinuousContenairization<MessageBody> {

	@Override
	public String type() {
		return "message_bodies";
	}

	private static final Supplier<Cache<String, MessageBody>> cacheHolder = Suppliers
			.memoize(() -> CacheRegistry.get().get("net.bluemind.backend.mail.replica.service.internal.BodiesCache"));

	@Override
	public void preCreate(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		loadAndSave(domainUid, ownerId, mailboxRecord, true);
	}

	private void loadAndSave(String domainUid, String ownerId, MailboxRecord mailboxRecord, boolean create) {
		if (targetStore().isPaused()) {
			return;
		}
		MessageBody messageBody = fetchMessageBody(domainUid, ownerId, mailboxRecord);
		save(domainUid, ownerId, messageBody.guid, messageBody, create);
	}

	@Override
	public void preUpdate(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		loadAndSave(domainUid, ownerId, mailboxRecord, false);
	}

	public static MessageBody fetchMessageBody(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		return Optional.ofNullable(cacheHolder.get().getIfPresent(mailboxRecord.messageBody))
				.orElseGet(() -> slowFetchMessageBody(domainUid, ownerId, mailboxRecord));
	}

	private static MessageBody slowFetchMessageBody(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDirectory directoryApi = prov.instance(IDirectory.class, domainUid);
		var mailbox = directoryApi.findByEntryUid(ownerId);
		String partition = CyrusPartition.forServerAndDomain(mailbox.dataLocation, domainUid).name;

		IDbMessageBodies apiMessageBodies = prov.instance(IDbMessageBodies.class, partition);
		return apiMessageBodies.getComplete(mailboxRecord.messageBody);
	}

}
