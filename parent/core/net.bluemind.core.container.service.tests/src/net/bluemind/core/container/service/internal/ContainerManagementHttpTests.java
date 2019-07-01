package net.bluemind.core.container.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;

public class ContainerManagementHttpTests extends ContainerManagementTests {

	protected IContainerManagement service(SecurityContext context, String containerUid) throws ServerFault {

		String sessionId = context.getSessionId();

		return ClientSideServiceProvider.getProvider("http://localhost:8090", sessionId)
				.instance(IContainerManagement.class, containerUid);
	}
}
