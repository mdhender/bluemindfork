package net.bluemind.core.container.api;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;

public interface IRestoreDirEntryWithMailboxSupport<T> extends IRestoreSupport<T> {

	ItemValue<T> getComplete(String uid);

	TaskRef delete(String uid);

}
