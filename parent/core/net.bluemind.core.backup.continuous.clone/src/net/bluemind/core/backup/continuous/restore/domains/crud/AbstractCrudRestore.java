package net.bluemind.core.backup.continuous.restore.domains.crud;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.RecordKey.Operation;
import net.bluemind.core.backup.continuous.restore.domains.RestoreDomainType;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.api.IRestoreSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;

public abstract class AbstractCrudRestore<T, U, V extends IRestoreSupport<U>> implements RestoreDomainType {

	protected final RestoreLogger log;
	protected final ItemValue<Domain> domain;

	protected AbstractCrudRestore(RestoreLogger log, ItemValue<Domain> domain) {
		this.log = log;
		this.domain = domain;
	}

	protected abstract ValueReader<ItemValue<T>> reader();

	protected abstract V api(ItemValue<Domain> domain, RecordKey key);

	@Override
	public void restore(RecordKey key, String payload) {
		V api = api(domain, key);
		if (Operation.isDelete(key)) {
			delete(key, payload, api);
		} else {
			createOrUpdate(key, payload, api);
		}
	}

	private void createOrUpdate(RecordKey key, String payload, V api) {
		ItemValue<T> item = reader().read(payload);
		if (filter(key, item)) {
			log.filter(type(), key);
			return;
		}
		boolean exists = api.get(item.uid) != null;
		if (exists) {
			log.update(type(), key);
			update(api, key, item);
		} else {
			log.create(type(), key);
			create(api, key, item);
		}
	}

	protected boolean filter(RecordKey key, ItemValue<T> item) {
		return false;
	}

	protected abstract ItemValue<U> map(ItemValue<T> item, boolean isCreate);

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

	protected void create(V api, RecordKey key, ItemValue<T> item) {
		ItemValue<U> toRestore = map(item, true);
		api.restore(toRestore, true);
	}

	protected void update(V api, RecordKey key, ItemValue<T> item) {
		ItemValue<U> toRestore = map(item, false);
		api.restore(toRestore, false);
	}

	protected abstract void delete(V api, RecordKey key, String uid);

}
