package net.bluemind.metrics.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import freemarker.template.Configuration;
import freemarker.template.Template;
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

	public static Template getTemplate(Class<?> sibling, String basePath, String templateName) throws IOException {
		Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		cfg.setClassForTemplateLoading(sibling, basePath);
		return cfg.getTemplate(templateName);
	}

	public static void write(IServer serverApi, ItemValue<Server> server, String dstFile, byte[] content) {
		serverApi.writeFile(server.uid, dstFile, content);
		if (logger.isInfoEnabled()) {
			logger.info("Created file {} at {}", dstFile, server.value.address());
		}
	}

	public static void jarToFS(Class<?> sibling, String src, String dest, ItemValue<Server> srvItem, IServer serverApi)
			throws IOException {

		try (InputStream in = sibling.getClassLoader().getResourceAsStream(src)) {
			write(serverApi, srvItem, dest, ByteStreams.toByteArray(in));
		}
	}

	public static boolean isCgroupV2(String serverIp) {
		INodeClient nodeClient = NodeActivator.get(serverIp);
		return nodeClient.exists("/sys/fs/cgroup/system.slice");
	}

	public static void deleteRemote(String serverIp, String file) {
		INodeClient nodeClient = NodeActivator.get(serverIp);
		NCUtils.execNoOut(nodeClient, 30, TimeUnit.SECONDS, "rm", "-f", file);
		logger.info("Deleted file {} at {}", file, serverIp);
	}

	public static void reloadTelegraf(String serverIp) {
		INodeClient nodeClient = NodeActivator.get(serverIp);
		NCUtils.execNoOut(nodeClient, 30, TimeUnit.SECONDS, "service", "telegraf", "restart");
	}
}
