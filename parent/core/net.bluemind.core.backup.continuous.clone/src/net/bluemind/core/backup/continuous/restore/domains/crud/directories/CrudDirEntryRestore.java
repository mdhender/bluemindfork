package net.bluemind.core.backup.continuous.restore.domains.crud.directories;

import java.util.Objects;

import net.bluemind.backend.mail.replica.utils.SubtreeContainerItemIdsCache;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.backup.continuous.restore.domains.crud.AbstractCrudRestore;
import net.bluemind.core.backup.continuous.tools.LockByKey;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.api.IRestoreItemCrudSupport;
import net.bluemind.core.container.api.IRestoreSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.domain.api.Domain;

public abstract class CrudDirEntryRestore<T, V extends IRestoreSupport<T>>
		extends AbstractCrudRestore<FullDirEntry<T>, T, V> {

	protected final IServiceProvider target;
	private final LockByKey<String> ouLock = new LockByKey<>();

	protected CrudDirEntryRestore(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target,
			RestoreState state) {
		super(log, domain, state);
		this.target = target;
	}

	@Override
	public String type() {
		return null;
	}

	@Override
	protected void create(V api, RecordKey key, VersionnedItem<FullDirEntry<T>> item) {
		registrerCyrusDependencies(item);
		ensureOrgUnitExists(item.value);
		super.create(api, key, item);
	}

	protected void ensureOrgUnitUidExists(String ouUid) {
		if (ouUid != null) {
			IOrgUnits ouApi = target.instance(IOrgUnits.class, domain.uid);
			try {
				ouLock.lock(ouUid);
				if (ouApi.get(ouUid) == null) {
					OrgUnit placeholder = new OrgUnit();
					placeholder.name = "placeholder of " + ouUid;
					log.monitor().log("Placeholder OrgUnit {}", ouUid);
					ouApi.create(ouUid, placeholder);
				}
			} finally {
				ouLock.unlock(ouUid);
			}
		}
	}

	protected void ensureOrgUnitExists(FullDirEntry<T> fde) {
		ensureOrgUnitUidExists(fde.entry.orgUnitUid);
	}

	@Override
	protected void update(V api, RecordKey key, VersionnedItem<FullDirEntry<T>> item) {
		registrerCyrusDependencies(item);
		super.update(api, key, item);
	}

	private void registrerCyrusDependencies(VersionnedItem<FullDirEntry<T>> item) {
		if (Objects.isNull(item.ids)) {
			return;
		}
		item.ids.forEach(dependency -> SubtreeContainerItemIdsCache.putFolderId(dependency.key, dependency.id));
	}

	@Override
	protected ItemValue<T> map(VersionnedItem<FullDirEntry<T>> item, boolean isCreate) {
		VersionnedItem<FullDirEntry<T>> fixedUp = fixup(item);
		return ItemValue.create(fixedUp.item(), fixedUp.value.value);
	}

	public abstract static class WithoutMailbox<T> extends CrudDirEntryRestore<T, IRestoreItemCrudSupport<T>> {

		protected WithoutMailbox(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target,
				RestoreState state) {
			super(log, domain, target, state);
		}

		@Override
		protected boolean exists(IRestoreItemCrudSupport<T> api, RecordKey key, VersionnedItem<FullDirEntry<T>> item) {
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
		protected void delete(IRestoreItemCrudSupport<T> api, RecordKey key, String uid) {
			api.delete(uid);
		}
	}

	public abstract static class WithMailbox<T> extends CrudDirEntryRestore<T, IRestoreDirEntryWithMailboxSupport<T>> {

		protected WithMailbox(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target,
				RestoreState state) {
			super(log, domain, target, state);
		}

		@Override
		protected boolean exists(IRestoreDirEntryWithMailboxSupport<T> api, RecordKey key,
				VersionnedItem<FullDirEntry<T>> item) {
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
		protected void delete(IRestoreDirEntryWithMailboxSupport<T> api, RecordKey key, String uid) {
			TaskRef ref = api.delete(uid);
			TaskUtils.logStreamWait(target, ref);
		}
	}
}