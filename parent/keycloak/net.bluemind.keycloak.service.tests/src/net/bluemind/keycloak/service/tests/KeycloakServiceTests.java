package net.bluemind.keycloak.service.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakKerberosAdmin;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.api.KerberosComponent;
import net.bluemind.keycloak.api.Realm;
import net.bluemind.keycloak.api.KerberosComponent.CachePolicy;
import net.bluemind.pool.impl.BmConfIni;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KeycloakServiceTests extends AbstractServiceTests {
	private static final Logger logger = LoggerFactory.getLogger(KeycloakServiceTests.class);
	
	protected static IKeycloakAdmin keycloakAdminService = null;
	protected static IKeycloakClientAdmin keycloakClientAdminService = null;
	protected static IKeycloakBluemindProviderAdmin keycloakBluemindProviderService = null;
	protected static IKeycloakKerberosAdmin keycloakKerberosService = null;
	protected static IDomains domainService = null;
	protected static String testRealmName = null;
	protected static String oidcClientName = null;
	
	@Test
	public void _000_instantiateServices() {
		keycloakAdminService = null;
		keycloakClientAdminService = null;
		keycloakBluemindProviderService = null;
		keycloakKerberosService = null;
		domainService = null;
		testRealmName = "rlm" + System.currentTimeMillis() + ".loc";
		oidcClientName = IKeycloakUids.clientId(testRealmName);
		
		keycloakAdminService = getKeycloakAdminService();
		assertNotNull("Unable to instantiate keycloakAdminService", keycloakAdminService);
		
		keycloakClientAdminService = getKeycloakClientAdminService();
		assertNotNull("Unable to instantiate keycloakClientAdminService", keycloakClientAdminService);
		
		keycloakBluemindProviderService = getKeycloakBluemindProviderService();
		assertNotNull("Unable to instantiate keycloakBluemindProviderService", keycloakBluemindProviderService);
		
		keycloakKerberosService = getKeycloakKerberosService();
		assertNotNull("Unable to instantiate keycloakKerberosService", keycloakKerberosService);
		
		domainService = getDomainService();
		assertNotNull("Unable to instantiate domainService", domainService);
		
		domainService.all();
	}
	
	@Test
	public void _010_createRealm() {
		assertNotNull("keycloakAdminService not correctly instantiated", keycloakAdminService);
		
		keycloakAdminService.createRealm(testRealmName);
	}
	
	@Test
	public void _020_allRealms() {
		assertNotNull("keycloakAdminService not correctly instantiated", keycloakAdminService);
		
		boolean foundMasterRealm = false;
		boolean foundTestRealm = false;
		List<Realm> lstRealms = keycloakAdminService.allRealms();
		for (int i=0 ; i < lstRealms.size() ; i++) {
			if ("master".equals(lstRealms.get(i).realm)) {
				foundMasterRealm = true;
			}
			if (testRealmName.equals(lstRealms.get(i).realm)) {
				foundTestRealm = true;
			}
		}
		assertTrue("Did not find Keycloak realms", foundMasterRealm && foundTestRealm);
	}
	
	@Test
	public void _030_getRealm() {
		assertNotNull("keycloakAdminService not correctly instantiated", keycloakAdminService);
		
		Realm testRealm = keycloakAdminService.getRealm(testRealmName);
		assertTrue("Unable to get test realm" , testRealm != null && testRealmName.equals(testRealm.realm));
	}
	
	@Test
	public void _040_oidcClient() {
		assertTrue("Services not correctly instantiated", keycloakAdminService != null && keycloakClientAdminService != null);
		
		keycloakClientAdminService.create(oidcClientName);
		assertNotNull("Unable to get oidc client secret", keycloakClientAdminService.getSecret(oidcClientName));
	}
	
	@Test
	public void _050_bluemindProvider() {
		assertNotNull("keycloakBluemindProviderService not correctly instantiated", keycloakBluemindProviderService);
		
		BluemindProviderComponent bpComponent = new BluemindProviderComponent();
		bpComponent.setParentId(testRealmName);
		bpComponent.setName(testRealmName + "-bmprovider");
		bpComponent.setBmUrl("http://" + getMyIpAddress() + ":8090"); //peut-etre ou peut-etre pas
		bpComponent.setBmCoreToken(securityContext.getSessionId());
		
		keycloakBluemindProviderService.create(bpComponent);
	}

	@Test
	public void _060_kerberosProvider() {
		assertNotNull("keycloakKerberosService not correctly instantiated", keycloakKerberosService);
		
		KerberosComponent kerb = new KerberosComponent();
		kerb.setKerberosRealm("TEST-DOMAIN.LOCAL");
		kerb.setServerPrincipal("HTTP/keycloak.test-domain.local@TEST-DOMAIN.LOCAL");
		kerb.setKeyTab("/tmp/keytab");
		kerb.setEnabled(true);
		kerb.setDebug(true);
		kerb.setCachePolicy(CachePolicy.DEFAULT);
		kerb.setName(testRealmName + "-kerberos");
		kerb.setParentId(testRealmName);

		keycloakKerberosService.create(kerb);
	}
	
	@Test
	public void _070_deleteRealm() {
		assertNotNull("keycloakAdminService not correctly instantiated", keycloakAdminService);
		
		keycloakAdminService.deleteRealm(testRealmName);
		assertNull("Unable to delete realm",keycloakAdminService.getRealm(testRealmName));
	}
	
	@Test 
	public void _080_domainHookOnCreate() {
		testRealmName = "dmn" + System.currentTimeMillis() + ".loc";
		oidcClientName = IKeycloakUids.clientId(testRealmName);
		keycloakClientAdminService = getKeycloakClientAdminService();
		assertTrue("Services not correctly instantiated", domainService != null && keycloakAdminService != null && keycloakClientAdminService != null);

		domainService.create(testRealmName, Domain.create(testRealmName, testRealmName, "Temporary test domain", new HashSet<String>()));
		try {
			keycloakAdminService.deleteRealm("global.virt");
		} catch (Throwable t) {
		}
		assertNotNull("Unable to create Bluemind domain", domainService.get(testRealmName));
		assertNotNull("Unable to find automaticalluy created kerberos realm", keycloakAdminService.getRealm(testRealmName));
		assertNotNull("Unable to get oidc client secret for automatically created realm", keycloakClientAdminService.getSecret(oidcClientName));
	}
	
	@Test 
	public void _090_domainHookOnDelete() {
		assertTrue("Services not correctly instantiated", domainService != null && keycloakAdminService != null);
		
		try {
			TaskRef taskRef = domainService.deleteDomainItems(testRealmName);
			TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), taskRef);
			assertTrue(status.state.succeed);
			domainService.delete(testRealmName);
			
		} catch (Throwable e) {
			logger.error("EXCeption " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		assertNull("Unable to delete Keycloak realm", keycloakAdminService.getRealm(testRealmName));
	}
	
	@Test
	public void _100_bmPluginWorks() {
		testRealmName = "dom" + System.currentTimeMillis() + ".loc";
		oidcClientName = IKeycloakUids.clientId(testRealmName);
		keycloakClientAdminService = getKeycloakClientAdminService();
		keycloakBluemindProviderService = getKeycloakBluemindProviderService();
		
		assertTrue("keycloakBluemindProviderService not correctly instantiated", 
				keycloakAdminService != null && keycloakClientAdminService != null && keycloakBluemindProviderService != null);
		
		assertTrue("Failed to create test domain", createDomainWithUser(testRealmName, "test.user", "somePassw0rd"));
		
		keycloakAdminService.deleteRealm(testRealmName);
		keycloakAdminService.createRealm(testRealmName);
		keycloakClientAdminService.create(oidcClientName);
		
		BluemindProviderComponent bpComponent = new BluemindProviderComponent();
		bpComponent.setParentId(testRealmName);
		bpComponent.setName(testRealmName + "-X" + securityContext.getSessionId());
		bpComponent.setBmUrl("http://" + getMyIpAddress() + ":8090"); //peut-etre ou peut-etre pas
		bpComponent.setBmCoreToken(securityContext.getSessionId());
		keycloakBluemindProviderService.create(bpComponent);
		
		String accessToken = null;
		try {
			String endpoint = "http://" + new BmConfIni().get("keycloak") + ":8099/realms/" + testRealmName + "/protocol/openid-connect/token";
			Builder requestBuilder = HttpRequest.newBuilder(new URI(endpoint));
			requestBuilder.header("Charset", StandardCharsets.UTF_8.name());
			requestBuilder.header("Content-Type", "application/x-www-form-urlencoded");
			String params = "grant_type=password";
			params += "&client_id=" + oidcClientName;
			params += "&client_secret=" + keycloakClientAdminService.getSecret(oidcClientName);
			params += "&username=test.user@" + testRealmName;
			params += "&password=somePassw0rd";
			byte[] postData = params.getBytes(StandardCharsets.UTF_8);
			requestBuilder.method("POST", HttpRequest.BodyPublishers.ofByteArray(postData));
			HttpRequest req = requestBuilder.build();
			HttpClient cli = HttpClient.newHttpClient();
			HttpResponse<String> resp = cli.send(req, BodyHandlers.ofString());

			if (resp.statusCode() >= 400) {
				assertTrue("Failed to authenticate with keycloak", false);
			} else {
				JsonObject result = new JsonObject(resp.body());
				accessToken = result.getString("access_token");
			}
		} catch (Exception e) {
			logger.error("EXCeption " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		assertNotNull("Failed to authenticate with keycloak", accessToken);
		
		try {
			keycloakAdminService.deleteRealm(testRealmName);
		} catch (Throwable t) {
		}
		
	}
	
	protected IKeycloakAdmin getKeycloakAdminService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IKeycloakAdmin.class);
	}
	
	protected IKeycloakClientAdmin getKeycloakClientAdminService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IKeycloakClientAdmin.class, testRealmName);
	}
	
	protected IKeycloakBluemindProviderAdmin getKeycloakBluemindProviderService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IKeycloakBluemindProviderAdmin.class, testRealmName);
	}
	
	protected IKeycloakKerberosAdmin getKeycloakKerberosService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IKeycloakKerberosAdmin.class, testRealmName);
	}
	
	protected IDomains getDomainService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IDomains.class);
	}
}
