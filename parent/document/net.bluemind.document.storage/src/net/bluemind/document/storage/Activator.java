package net.bluemind.document.storage;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class Activator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);

	private static BundleContext context;
	private static IDocumentStore store;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;

		RunnableExtensionLoader<IDocumentStore> rel = new RunnableExtensionLoader<IDocumentStore>();
		List<IDocumentStore> stores = rel.loadExtensions("net.bluemind.document", "documentstore", "document_store",
				"implementation");

		int priority = 0;
		for (IDocumentStore ids : stores) {
			if (ids.getPriority() > priority) {
				store = ids;
				priority = ids.getPriority();
			}
		}

		if (store == null) {
			logger.error("No IDocumentStore implementation found!");
		} else {
			logger.info("IDocumentStore implementation: {}", store.getClass());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

	public static IDocumentStore getDocumentStore() {
		return store;
	}
}
