package net.bluemind.core.backup.continuous.restore.domains.crud.directories;

import java.util.Objects;

import net.bluemind.backend.mail.replica.utils.SubtreeContainerItemIdsCache;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.crud.AbstractCrudRestore;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.api.IRestoreItemCrudSupport;
import net.bluemind.core.container.api.IRestoreSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.domain.api.Domain;

public abstract class CrudDirEntryRestore<T, V extends IRestoreSupport<T>>
		extends AbstractCrudRestore<FullDirEntry<T>, T, V> {

	protected CrudDirEntryRestore(RestoreLogger log, ItemValue<Domain> domain) {
		super(log, domain);
	}

	@Override
	public String type() {
		return null;
	}

	@Override
	protected void create(V api, RecordKey key, VersionnedItem<FullDirEntry<T>> item) {
		registrerCyrusDependencies(item);
		super.create(api, key, item);
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
		return ItemValue.create(item.item(), item.value.value);
	}

	public abstract static class WithoutMailbox<T> extends CrudDirEntryRestore<T, IRestoreItemCrudSupport<T>> {

		protected WithoutMailbox(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
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

		private final IServiceProvider target;

		protected WithMailbox(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
			super(log, domain);
			this.target = target;
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