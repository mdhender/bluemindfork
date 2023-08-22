package net.bluemind.startup.services.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDependenciesLookup {
	private static final Logger logger = LoggerFactory.getLogger(ServiceDependenciesLookup.class);

	public static final Map<Bundle, Boolean> bundleStarting = new ConcurrentHashMap<>();
	public static final Map<Class<?>, Boolean> serviceLoaded = new ConcurrentHashMap<>();

	private ServiceDependenciesLookup() {

	}

	public static void startBundleImplementing(BundleContext bundleContext, Class<?> serviceName) {
		if (serviceLoaded.containsKey(serviceName) && serviceLoaded.get(serviceName)) {
			return;
		}

		Arrays.asList(bundleContext.getBundles()).stream() //
				.filter(bundle -> !bundleStarting.containsKey(bundle)) //
				.filter(bundle -> !bundle.getSymbolicName().equals(bundleContext.getBundle().getSymbolicName())) //
				.filter(bundle -> bundleExportService(bundle, serviceName)) //
				.forEach(bundle -> startBundle(bundle, serviceName.getCanonicalName()));

		serviceLoaded.put(serviceName, true);
	}

	private static boolean bundleExportService(Bundle bundle, Class<?> serviceName) {
		String exportServiceValue = bundle.getHeaders().get("Export-Service");
		if (exportServiceValue == null) {
			return false;
		}
		Set<String> exported = Arrays.asList(exportServiceValue.split(",")).stream().map(String::trim)
				.collect(Collectors.toSet());
		return exported.contains(serviceName.getCanonicalName());
	}

	private static void startBundle(Bundle bundle, String serviceName) {
		try {
			logger.info("Starting bundle '{}' to load '{}' implementations", bundle.getSymbolicName(), serviceName);
			bundle.start();
		} catch (BundleException e) {
			logger.error("Unable to load bundle '{}' which export a required service '{}' that will not be injected",
					bundle.getSymbolicName(), serviceName, e);
		}
	}
}
