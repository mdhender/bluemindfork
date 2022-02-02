package net.bluemind.core.backup.continuous.restore.domains.crud.directories;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.crud.AbstractCrudRestore;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.api.IRestoreSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public abstract class CrudDirEntryRestore<T, V extends IRestoreSupport<T>> extends AbstractCrudRestore<FullDirEntry<T>, T, V> {

	protected CrudDirEntryRestore(RestoreLogger log, ItemValue<Domain> domain) {
		super(log, domain);
	}

	@Override
	public String type() {
		return null;
	}

	protected ItemValue<T> map(ItemValue<FullDirEntry<T>> item, boolean isCreate) {
		return ItemValue.create(item.item(), item.value.value);
	}

	public abstract static class WithoutMailbox<T> extends CrudDirEntryRestore<T, IRestoreCrudSupport<T>> {

		protected WithoutMailbox(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		protected void delete(IRestoreCrudSupport<T> api, RecordKey key, String uid) {
			api.delete(uid);
		}
	}

	public abstract static class WithMailbox<T> extends CrudDirEntryRestore<T, IRestoreDirEntryWithMailboxSupport<T>> {

		protected WithMailbox(RestoreLogger log, ItemValue<Domain> domain) {
			super(log, domain);
		}

		protected void delete(IRestoreDirEntryWithMailboxSupport<T> api, RecordKey key, String uid) {
			api.delete(uid);
		}
	}
}