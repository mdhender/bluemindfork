package net.bluemind.keycloak.service.tests;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.domain.api.IDomains;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakKerberosAdmin;

public class KeycloakServiceHttpTests extends KeycloakServiceTests {
	protected IKeycloakAdmin getKeycloakAdminService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId()).instance(IKeycloakAdmin.class);
	}
	
	protected IKeycloakClientAdmin getKeycloakClientAdminService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId()).instance(IKeycloakClientAdmin.class, testRealmName);
	}
	
	protected IKeycloakBluemindProviderAdmin getKeycloakBluemindProviderService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId()).instance(IKeycloakBluemindProviderAdmin.class, testRealmName);
	}
	
	protected IKeycloakKerberosAdmin getKeycloakKerberosService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId()).instance(IKeycloakKerberosAdmin.class, testRealmName);
	}
	
	protected IDomains getDomainService() throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://localhost:8090", securityContext.getSessionId()).instance(IDomains.class);
	}
}
