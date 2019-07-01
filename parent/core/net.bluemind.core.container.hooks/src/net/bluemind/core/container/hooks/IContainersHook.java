package net.bluemind.core.container.hooks;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.rest.BmContext;

public interface IContainersHook {

	void onContainerCreated(BmContext ctx, ContainerDescriptor cd) throws ServerFault;

	void onContainerUpdated(BmContext ctx, ContainerDescriptor prev, ContainerDescriptor cur) throws ServerFault;

	void onContainerDeleted(BmContext ctx, ContainerDescriptor cd) throws ServerFault;

	void onContainerSubscriptionsChanged(BmContext ctx, ContainerDescriptor cd, List<String> subs, List<String> unsubs)
			throws ServerFault;

	void onContainerOfflineSyncStatusChanged(BmContext ctx, ContainerDescriptor cd, String subject);

	void onContainerSettingsChanged(BmContext ctx, ContainerDescriptor cd) throws ServerFault;

}
