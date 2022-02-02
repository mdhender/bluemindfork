package net.bluemind.core.container.api;

import net.bluemind.core.container.model.ItemValue;

public interface IRestoreSupport<T> {

	T get(String uid);

	void restore(ItemValue<T> item, boolean isCreate);

}
