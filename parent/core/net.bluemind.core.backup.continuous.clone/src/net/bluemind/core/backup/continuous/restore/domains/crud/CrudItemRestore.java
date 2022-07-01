package net.bluemind.core.backup.continuous.restore.domains.crud;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.api.IRestoreItemCrudSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public abstract class CrudItemRestore<T> extends AbstractCrudRestore<T, T, IRestoreItemCrudSupport<T>> {

	protected CrudItemRestore(RestoreLogger log, ItemValue<Domain> domain) {
		super(log, domain);
	}

	@Override
	protected boolean exists(IRestoreItemCrudSupport<T> api, RecordKey key, VersionnedItem<T> item) {
		ItemValue<T> previous = api.getComplete(item.uid);
		if (previous != null && previous.internalId != item.internalId) {
			log.deleteByProduct(type(), key);
			delete(api, key, item.uid);
			return false;
		} else {
			return previous != null;
		}
	}

	@Override
	protected ItemValue<T> map(VersionnedItem<T> item, boolean isCreate) {
		return item;
	}

	@Override
	protected void create(IRestoreItemCrudSupport<T> api, RecordKey key, VersionnedItem<T> item) {
		try {
			super.create(api, key, item);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
				log.failureIgnored(type(), key, "Item already exists and can't be created, trying to delete."
						+ "This is probably due to an asynchronous item creation. This should be handled on the API side.");
				createOrUpdate(api, key, item);
			}
		}
	}

	@Override
	protected void delete(IRestoreItemCrudSupport<T> api, RecordKey key, String uid) {
		api.delete(uid);
	}
}
