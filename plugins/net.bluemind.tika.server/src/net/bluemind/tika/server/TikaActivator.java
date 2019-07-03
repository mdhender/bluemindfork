package net.bluemind.tika.server;

import org.apache.tika.parser.Parser;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class TikaActivator implements BundleActivator, ServiceTrackerCustomizer {

	private static final Logger logger = LoggerFactory.getLogger(TikaActivator.class);
	private ServiceTracker parserTracker;

	// The plug-in ID
	public static final String PLUGIN_ID = "net.bluemind.tika.server"; //$NON-NLS-1$

	// The shared instance
	private static TikaActivator plugin;

	/**
	 * The constructor
	 */
	public TikaActivator() {
	}

	public void start(BundleContext context) throws Exception {
		plugin = this;
		parserTracker = new ServiceTracker(context, Parser.class.getName(), this);

		parserTracker.open();

	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static TikaActivator getDefault() {
		return plugin;
	}

	public Object addingService(ServiceReference reference) {
		logger.info("ref ser {} {}", reference, reference.getBundle());
		return null;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
		logger.info("remove ser {} {}", reference, reference.getBundle());

	}

}
