package net.bluemind.server.mcast.hook;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.node.client.AHCNodeClientFactory;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class MCastHook extends DefaultServerHook {
	private static final Logger logger = LoggerFactory.getLogger(MCastHook.class);

	private AHCNodeClientFactory ncr;

	public MCastHook() {
		this.ncr = new AHCNodeClientFactory();
	}

	@Override
	public void onServerCreated(BmContext context, ItemValue<Server> server) {

		File f = new File("/etc/bm/mcast.id");
		if (!f.exists()) {
			logger.info("server created: generating mcast.id");
			try {
				Files.write(UUID.randomUUID().toString().getBytes(), f);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				return;
			}
		}
		String adr = server.value.address();
		try {
			logger.info("server created: copy mcast.id to {}", adr);
			INodeClient remote = NodeActivator.get(adr);
			byte[] data = Files.toByteArray(f);
			remote.writeFile(f.getAbsolutePath(), new ByteArrayInputStream(data));
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {

		if (tag.equals("bm/es")) {
			updateESClusterName(server, tag);
		}
	}

	private void updateESClusterName(ItemValue<Server> server, String tag) {
		logger.info("node {} tagged elasticsearch:[{}]", server.value.address(), tag);
		try {
			String adr = server.value.address();
			INodeClient nc = ncr.create(adr);

			String yml = new String(nc.read("/usr/share/bm-elasticsearch/config/elasticsearch.yml"));

			String mcastId = new String(Files.toByteArray(new File("/etc/bm/mcast.id")));

			String clusterName = "cluster.name: bluemind-" + mcastId + "\n";

			String ymlToWrite = yml.replaceAll("cluster.name: bluemind(.*)\n", clusterName);
			if (ymlToWrite.equals(yml)) {
				logger.info("elasticsearch is already configured, skip it");
				return;
			}

			nc.writeFile("/usr/share/bm-elasticsearch/config/elasticsearch.yml",
					new ByteArrayInputStream(ymlToWrite.getBytes()));

			logger.info("elasticsearch is configured on node {}, restart", adr);
			ExitList el = NCUtils.exec(nc, "service bm-elasticsearch restart");
			for (String out : el) {
				logger.info("ES: {}", out);
			}
			logger.info("ElasticSearch restarted, exit code: {}", el.getExitCode());

		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
