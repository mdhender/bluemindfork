package net.bluemind.core.container.api;

import net.bluemind.core.task.api.TaskRef;

public interface IRestoreDirEntryWithMailboxSupport<T> extends IRestoreSupport<T> {

	TaskRef delete(String uid);

}
