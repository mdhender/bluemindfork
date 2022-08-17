package net.bluemind.metrics.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class TagHelper {

	public static Logger logger = LoggerFactory.getLogger(TagHelper.class);

	private TagHelper() {
	}

	public static void jarToFS(Class<?> sibling, String src, String dest, ItemValue<Server> srvItem, IServer serverApi)
			throws IOException {

		try (InputStream in = sibling.getClassLoader().getResourceAsStream(src)) {
			serverApi.writeFile(srvItem.uid, dest, ByteStreams.toByteArray(in));
			if (logger.isInfoEnabled()) {
				logger.info("Created file {} at {}", dest, srvItem.value.address());
			}
		}
	}

	public static void deleteRemote(String serverIp, String file) {
		INodeClient nodeClient = NodeActivator.get(serverIp);
		NCUtils.execNoOut(nodeClient, "rm -f " + file, 30, TimeUnit.SECONDS);
		logger.info("Deleted file {} at {}", file, serverIp);
	}

	public static void reloadTelegraf(String serverIp) {
		INodeClient nodeClient = NodeActivator.get(serverIp);
		NCUtils.execNoOut(nodeClient, "service telegraf restart", 30, TimeUnit.SECONDS);
	}
}
