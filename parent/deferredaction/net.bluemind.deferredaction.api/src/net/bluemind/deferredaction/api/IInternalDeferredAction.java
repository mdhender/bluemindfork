package net.bluemind.deferredaction.api;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IRestoreItemCrudSupport;

public interface IInternalDeferredAction extends IDeferredAction, IRestoreItemCrudSupport<DeferredAction> {

	/**
	 * Creates a new {@link DeferredAction} and compute it's unique id from the
	 * deferred action data
	 * 
	 * @param deferredAction deferred action data
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	public void create(DeferredAction deferredAction) throws ServerFault;
}
