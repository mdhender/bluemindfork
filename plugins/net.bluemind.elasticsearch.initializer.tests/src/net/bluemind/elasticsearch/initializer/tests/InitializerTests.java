package net.bluemind.elasticsearch.initializer.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Properties;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.elasticsearch.initializer.ElasticSearchServerHook;
import net.bluemind.lib.elasticsearch.ESearchActivator;
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
		Client client = ESearchActivator.createClient(Arrays.asList(elasticSearchHost));
		IndicesExistsResponse resp = client.admin().indices().prepareExists(TestInitializer.indexName).execute()
				.actionGet();
		assertTrue(resp.isExists());
	}

}
