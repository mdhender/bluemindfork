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
import net.bluemind.mailbox.api.IMailboxes;

public class MessageBodyHook implements IMessageBodyHook, ContinuousContenairization<MessageBody> {

	@Override
	public String type() {
		return "message_bodies";
	}

	private static final Supplier<Cache<String, MessageBody>> cacheHolder = Suppliers
			.memoize(() -> CacheRegistry.get().get("net.bluemind.backend.mail.replica.service.internal.BodiesCache"));

	@Override
	public void preCreate(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		MessageBody messageBody = fetchMessageBody(domainUid, ownerId, mailboxRecord);
		save(domainUid, ownerId, messageBody.guid, messageBody, true);
	}

	@Override
	public void preUpdate(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		MessageBody messageBody = fetchMessageBody(domainUid, ownerId, mailboxRecord);
		save(domainUid, ownerId, messageBody.guid, messageBody, false);
	}

	public static MessageBody fetchMessageBody(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		return Optional.ofNullable(cacheHolder.get().getIfPresent(mailboxRecord.messageBody))
				.orElseGet(() -> slowFetchMessageBody(domainUid, ownerId, mailboxRecord));
	}

	private static MessageBody slowFetchMessageBody(String domainUid, String ownerId, MailboxRecord mailboxRecord) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IMailboxes mailboxesApi = prov.instance(IMailboxes.class, domainUid);
		var mailbox = mailboxesApi.getComplete(ownerId);
		String partition = CyrusPartition.forServerAndDomain(mailbox.value.dataLocation, domainUid).name;

		IDbMessageBodies apiMessageBodies = prov.instance(IDbMessageBodies.class, partition);
		return apiMessageBodies.getComplete(mailboxRecord.messageBody);
	}

}
