package net.bluemind.core.container.api;

import net.bluemind.core.container.model.ItemValue;

public interface IRestoreItemCrudSupport<T> extends IRestoreCrudSupport<T> {

	ItemValue<T> getComplete(String uid);

}
