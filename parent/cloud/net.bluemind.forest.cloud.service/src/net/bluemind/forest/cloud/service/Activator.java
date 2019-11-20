package net.bluemind.forest.cloud.service;

import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.hazelcast.core.HazelcastInstance;

import net.bluemind.forest.cloud.hazelcast.HzStarter;
import net.bluemind.kafka.configuration.Brokers;

public class Activator implements BundleActivator {

	private static BundleContext context;
	private static HazelcastInstance hazelcast;

	static BundleContext getContext() {
		return context;
	}

	static HazelcastInstance getHazelcast() {
		return hazelcast;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;

		HzStarter starter = new HzStarter("forest-node", Brokers.locate().inspectAddress());
		Activator.hazelcast = starter.get(30, TimeUnit.SECONDS);

		ForestJoinService.init();

	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}
