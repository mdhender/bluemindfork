package net.bluemind.backend.mail.replica.service;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

import javax.sql.DataSource;

import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.hook.IMessageBodyHook;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.container.persistence.IWeightProvider;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.ItemValueAuditLogService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class HookMailboxRecordStoreService extends ContainerStoreService<MailboxRecord> {

	private static List<IMessageBodyHook> hooks = getHooks();

	public HookMailboxRecordStoreService(DataSource pool, SecurityContext securityContext, Container container,
			IItemValueStore<MailboxRecord> itemValueStore, IItemFlagsProvider<MailboxRecord> fProv,
			IWeightSeedProvider<MailboxRecord> wsProv, IWeightProvider wProv,
			ItemValueAuditLogService<MailboxRecord> logService) {
		super(pool, securityContext, container, itemValueStore, fProv, wsProv, wProv, logService);
	}

	@Override
	protected void beforeCreationInBackupStore(ItemValue<MailboxRecord> itemValue) {
		hooks.forEach(hook -> hook.preCreate(container.domainUid, container.owner, itemValue.value));
	}

	@Override
	protected void preUpdateValue(Item newItem, MailboxRecord newValue, Supplier<MailboxRecord> oldValue)
			throws SQLException {
		MailboxRecord prevRec = oldValue.get();
		if (!prevRec.messageBody.equals(newValue.messageBody)) {
			hooks.forEach(hook -> hook.preUpdate(container.domainUid, container.owner, newValue));
		}

	}

	private static List<IMessageBodyHook> getHooks() {
		RunnableExtensionLoader<IMessageBodyHook> loader = new RunnableExtensionLoader<>();
		return loader.loadExtensions("net.bluemind.backend.mail.replica.hook", "messagebodyhook", "hook", "impl");
	}

}
