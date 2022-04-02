package net.bluemind.core.backup.continuous.events;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;

public interface ContinuousContenairization<T> {

	static final Logger logger = LoggerFactory.getLogger(ContinuousContenairization.class);

	String type();

	default IBackupStoreFactory targetStore() {
		return DefaultBackupStore.store();
	}

	default void save(String domainUid, String ownerUid, String itemUid, T value, boolean created) {
		ItemValue<T> iv = itemValue(itemUid, value, created);
		save(domainUid, ownerUid, iv.item(), value);
	}

	default void save(String domainUid, String ownerUid, Item item, T value) {
		ContainerDescriptor metaDesc = descriptor(domainUid, ownerUid);
		ItemValue<T> iv = ItemValue.create(item, value);
		targetStore().<T>forContainer(metaDesc).store(iv).whenComplete((v, ex) -> log("Save", metaDesc, iv, ex));
	}

	default void delete(String domainUid, String ownerUid, String itemUid, T previous) {
		ContainerDescriptor metaDesc = descriptor(domainUid, ownerUid);
		ItemValue<T> iv = itemValue(itemUid, previous, false);
		targetStore().<T>forContainer(metaDesc).delete(iv).whenComplete((v, ex) -> log("Delete", metaDesc, iv, ex));
	}

	default void log(String operation, ContainerDescriptor metaDesc, ItemValue<T> iv, Throwable ex) {
		if (ex != null) {
			logger.error("{}:fails type:{} domainUid:{} ownerUid:{} itemUid:{}", operation, type(), metaDesc.domainUid,
					metaDesc.owner, iv.uid, ex);
		} else {
			logger.info("{}:succeed type:{} domainUid:{} ownerUid:{} itemUid:{}", operation, type(), metaDesc.domainUid,
					metaDesc.owner, iv.uid);
		}
	}

	default ContainerDescriptor descriptor(String domainUid, String ownerUid) {
		return ContainerDescriptor.create(ownerUid + "_at_" + domainUid + "_" + type(), ownerUid + " " + type(),
				ownerUid, type(), domainUid, true);
	}

	default ItemValue<T> itemValue(String uid, T identity, boolean created) {
		ItemValue<T> iv = ItemValue.create(uid, identity);
		iv.internalId = iv.uid.hashCode();
		if (created) {
			iv.created = new Date();
		} else {
			iv.updated = new Date();
		}
		return iv;
	}

}
