package net.bluemind.core.backup.continuous.restore.domains.crud;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.RecordKey.Operation;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreDomainType;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.backup.continuous.tools.Locks;
import net.bluemind.core.container.api.IRestoreSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;

public abstract class AbstractCrudRestore<T, U, V extends IRestoreSupport<U>> implements RestoreDomainType {

	protected final RestoreLogger log;
	protected final ItemValue<Domain> domain;
	protected final RestoreState state;

	protected AbstractCrudRestore(RestoreLogger log, ItemValue<Domain> domain, RestoreState state) {
		this.log = log;
		this.domain = domain;
		this.state = state;
	}

	protected abstract ValueReader<VersionnedItem<T>> reader();

	protected abstract V api(ItemValue<Domain> domain, RecordKey key);

	@Override
	public void restore(RecordKey key, String payload) {
		V api;
		try {
			api = api(domain, key);
		} catch (ServerFault sf) {
			if (sf.getCode().equals(ErrorCode.NOT_FOUND) && Operation.isDelete(key)) {
				// It's safe to ignore a user not present before in the stream
				return;
			} else {
				throw sf;
			}
		}
		if (Operation.isDelete(key)) {
			delete(key, payload, api);
		} else {
			filterCreateOrUpdate(key, payload, api);
		}
	}

	protected void filterCreateOrUpdate(RecordKey key, String payload, V api) {
		VersionnedItem<T> item = fixup(reader().read(payload));
		if (filter(key, item)) {
			log.filter(type(), key);
			return;
		}
		createOrUpdate(api, key, item);
	}

	protected final void createOrUpdate(V api, RecordKey key, VersionnedItem<T> item) {
		String lockKey = Locks.key(domain.uid, item.uid);
		try {
			Locks.GLOBAL.lock(lockKey);
			boolean exists = exists(api, key, item);
			if (exists) {
				log.update(type(), key);
				update(api, key, item);
			} else {
				log.create(type(), key);
				create(api, key, item);
			}
		} finally {
			Locks.GLOBAL.unlock(lockKey);
		}
	}

	protected boolean filter(RecordKey key, VersionnedItem<T> item) {
		return false;
	}

	protected boolean exists(V api, RecordKey key, VersionnedItem<T> item) {
		return api.get(item.uid) != null;
	}

	protected VersionnedItem<T> fixup(VersionnedItem<T> item) {
		return item;
	}

	protected abstract ItemValue<U> map(VersionnedItem<T> item, boolean isCreate);

	private void delete(RecordKey key, String payload, V api) {
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

	protected void create(V api, RecordKey key, VersionnedItem<T> item) {
		ItemValue<U> toRestore = map(item, true);
		api.restore(toRestore, true);
	}

	protected void update(V api, RecordKey key, VersionnedItem<T> item) {
		ItemValue<U> toRestore = map(item, false);
		api.restore(toRestore, false);
	}

	protected abstract void delete(V api, RecordKey key, String uid);

}
