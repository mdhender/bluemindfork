package net.bluemind.node.api;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

/**
 * The activator class controls the plug-in life cycle
 */
public class NodeActivator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(NodeActivator.class);
	private static BundleContext context;

	private static final Supplier<INodeClient> localNodeClient = LocalNodeClient::new;

	static BundleContext getContext() {
		return context;
	}

	static void setContext(BundleContext bundleContext) {
		NodeActivator.context = bundleContext;
	}

	public void start(BundleContext bundleContext) throws Exception {
		setContext(bundleContext);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		setContext(null);
	}

	private static INodeClientFactory ncf = factory();

	private static INodeClientFactory factory() {
		RunnableExtensionLoader<INodeClientFactory> rel = new RunnableExtensionLoader<>();
		List<INodeClientFactory> ncfs = rel.loadExtensions("net.bluemind.node.api", "nodeclientfactory",
				"node_client_factory", "impl");

		if (ncfs == null || ncfs.isEmpty()) {
			logger.warn("no nodeClientFactory plugin. System ops will not be available");
			return null;
		}

		Collections.sort(ncfs, (INodeClientFactory o1, INodeClientFactory o2) -> {
			// biggest priority first
			return Integer.compare(o2.getPriority(), o1.getPriority());
		});
		return ncfs.get(0);
	}

	public static boolean requiresLocalNode(String host) {
		return Splitter.on(",").omitEmptyStrings().splitToStream(System.getProperty("node.local.ipaddr", ""))
				.anyMatch(host::equals);
	}

	public static INodeClient get(String host) throws ServerFault {
		if (requiresLocalNode(host)) {
			logger.info("Using local-node for {}", host);
			return localNodeClient.get();
		}

		if (ncf != null) {
			return ncf.create(host);
		} else {
			throw new ServerFault("No node factory loaded.");
		}
	}

}
