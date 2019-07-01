package net.bluemind.tracing.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Tracer;

public class TracerActivator implements BundleActivator {

	private static BundleContext context;
	private static Tracer tracerImpl;
	private static final Logger logger = LoggerFactory.getLogger(TracerActivator.class);

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		TracerActivator.context = bundleContext;
		logger.info("Loading tracer...");
		tracerImpl = configureJaegerTracer();
	}

	private JaegerTracer configureJaegerTracer() {
		JaegerTracer jaeger = Configuration
				.fromEnv(System.getProperty("net.bluemind.property.product", "unknown-service")).getTracer();
		logger.info("Tracer loaded {}", jaeger);
		return jaeger;
	}

	public static Tracer tracer() {
		return tracerImpl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		TracerActivator.context = null;
	}

}
