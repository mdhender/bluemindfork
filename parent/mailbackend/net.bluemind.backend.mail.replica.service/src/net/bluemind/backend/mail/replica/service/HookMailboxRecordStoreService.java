package net.bluemind.backend.mail.replica.service;

import java.util.List;

import javax.sql.DataSource;

import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.hook.IMessageBodyHook;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.container.persistence.IWeightProvider;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class HookMailboxRecordStoreService<T> extends ContainerStoreService<T> {

	private static List<IMessageBodyHook> hooks = getHooks();
	private String mailboxUniqueId;

	public HookMailboxRecordStoreService(DataSource pool, SecurityContext securityContext, Container container,
			IItemValueStore<T> itemValueStore, IItemFlagsProvider<T> fProv, IWeightSeedProvider<T> wsProv,
			IWeightProvider wProv, String mailboxUniqueId) {
		super(pool, securityContext, container, itemValueStore, fProv, wsProv, wProv);
		this.mailboxUniqueId = mailboxUniqueId;

	}

	@Override
	public ItemVersion createWithId(String uid, Long internalId, String extId, String displayName, T value) {
		MailboxRecord mailboxRecord = (MailboxRecord) value;
		hooks.forEach(hook -> hook.preCreate(container.domainUid, container.owner, mailboxRecord));
		return super.createWithId(uid, internalId, extId, displayName, value, changelogStore, itemStore,
				itemValueStore);
	}

	@Override
	public ItemVersion create(String uid, String displayName, T value) {
		MailboxRecord mailboxRecord = (MailboxRecord) value;
		hooks.forEach(hook -> hook.preCreate(container.domainUid, container.owner, mailboxRecord));
		return super.createWithId(uid, null, null, displayName, value);
	}

	@Override
	public ItemVersion create(Item item, T value) {
		MailboxRecord mailboxRecord = (MailboxRecord) value;
		hooks.forEach(hook -> hook.preCreate(container.domainUid, container.owner, mailboxRecord));
		return super.create(item, value);
	}

	@Override
	public ItemVersion update(long itemId, String displayName, T value) {
		MailboxRecord mailboxRecord = (MailboxRecord) value;
		hooks.forEach(hook -> hook.preUpdate(container.domainUid, container.owner, mailboxRecord));
		return super.update(itemId, displayName, value);
	}

	@Override
	public ItemVersion update(String uid, String displayName, T value) {
		MailboxRecord mailboxRecord = (MailboxRecord) value;
		hooks.forEach(hook -> hook.preUpdate(container.domainUid, container.owner, mailboxRecord));
		Item item = new Item();
		item.uid = uid;
		return super.update(item, displayName, value);
	}

	@Override
	public ItemVersion update(Item item, String displayName, T value) {
		MailboxRecord mailboxRecord = (MailboxRecord) value;
		hooks.forEach(hook -> hook.preUpdate(container.domainUid, container.owner, mailboxRecord));
		return super.update(item, displayName, value);
	}

	private static List<IMessageBodyHook> getHooks() {
		RunnableExtensionLoader<IMessageBodyHook> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.backend.mail.replica.hook", "messagebodyhook", "hook", "impl");
	}
}
