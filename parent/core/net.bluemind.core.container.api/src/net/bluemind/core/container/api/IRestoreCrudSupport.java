package net.bluemind.core.container.api;

public interface IRestoreCrudSupport<T> extends IRestoreSupport<T> {

	void delete(String uid);

}
