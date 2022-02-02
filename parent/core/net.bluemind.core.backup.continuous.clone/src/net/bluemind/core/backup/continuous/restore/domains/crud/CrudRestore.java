package net.bluemind.core.backup.continuous.restore.domains.crud;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public abstract class CrudRestore<T> extends AbstractCrudRestore<T, T, IRestoreCrudSupport<T>> {

	protected CrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
		super(log, domain);
	}

	@Override
	protected ItemValue<T> map(ItemValue<T> item, boolean isCreate) {
		return item;
	}

	@Override
	protected void delete(IRestoreCrudSupport<T> api, RecordKey key, String uid) {
		api.delete(uid);
	}
}
