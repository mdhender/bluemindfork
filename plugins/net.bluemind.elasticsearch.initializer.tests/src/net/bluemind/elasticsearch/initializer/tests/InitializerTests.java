package net.bluemind.elasticsearch.initializer.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Properties;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.Test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.elasticsearch.initializer.ElasticSearchServerHook;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.elasticsearch.ESearchActivator.Authentication;
import net.bluemind.lib.elasticsearch.ESearchActivator.AuthenticationCredential;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.IServerHook;

public class InitializerTests {

	private String elasticSearchHost;

	@Before
	public void setup() throws Exception {
		Properties p = new Properties();
		p.load(getClass().getClassLoader().getResourceAsStream("data/test.properties"));
		elasticSearchHost = (String) p.get("elasticsearch");
	}

	@Test
	public void testInitializers() throws Exception {
		TestInitializer.indexName = "test-" + System.currentTimeMillis();

		IExtensionPoint ep = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.elasticsearch",
				"initializer");

		assertNotNull(ep);

		IServerHook hook = new ElasticSearchServerHook();
		Server srv = new Server();
		srv.ip = elasticSearchHost;
		ItemValue<Server> iv = ItemValue.create(Item.create(null, null), srv);
		hook.onServerTagged(null, iv, "tag/test");

		// test index existance
		ElasticsearchClient client = ESearchActivator.getClient(Arrays.asList(elasticSearchHost),
				new AuthenticationCredential(Authentication.NONE));
		boolean exists = client.indices().exists(e -> e.index(TestInitializer.indexName)).value();
		assertTrue(exists);
	}

}
