/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.xmpp.server;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.systemd.notify.Startup;
import tigase.osgi.ModulesManager;

public class Activator implements BundleActivator, ServiceListener {

	private static Logger logger = LoggerFactory.getLogger(Activator.class);
	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	private ModulesManager serviceManager = null;
	private ServiceReference<ModulesManager> serviceReference = null;

	public static void loadTrick() {
		logger.info("xmpp.server activator.");
	}

	@Override
	public void serviceChanged(ServiceEvent event) {
		if (event.getType() == ServiceEvent.REGISTERED) {
			if (serviceReference == null) {
				serviceReference = (ServiceReference<ModulesManager>) event.getServiceReference();
				serviceManager = (ModulesManager) context.getService(serviceReference);
				registerAddons();
			}
		} else if (event.getType() == ServiceEvent.UNREGISTERING) {
			if (serviceReference == event.getServiceReference()) {
				unregisterAddons();
				context.ungetService(serviceReference);
				serviceManager = null;
				serviceReference = null;
			}
		}
	}

	@Override
	public void start(BundleContext bc) throws Exception {

		Activator.context = bc;
		CountDownLatch cdl = new CountDownLatch(2);
		VertxPlatform.spawnVerticles(r -> {
			logger.info("vertx platform started");
			cdl.countDown();
		});
		MQSetup.init(cdl);

		System.setProperty("tigase-configurator", BMConfigurator.class.getName());

		synchronized (this) {
			context = bc;
			bc.addServiceListener(this, "(&(objectClass=" + ModulesManager.class.getName() + "))");
			serviceReference = (ServiceReference<ModulesManager>) bc
					.getServiceReference(ModulesManager.class.getName());
			if (serviceReference != null) {
				serviceManager = (ModulesManager) bc.getService(serviceReference);
				registerAddons();
			}
		}

		Bundle tigaseServer = Platform.getBundle("tigase-server");
		if (tigaseServer.getState() == Bundle.RESOLVED) {
			tigaseServer.start(Bundle.START_TRANSIENT);
		}

		Bundle tigaseMuc = Platform.getBundle("tigase.muc");
		if (tigaseMuc.getState() == Bundle.RESOLVED) {
			tigaseMuc.start(Bundle.START_TRANSIENT);
		}
		cdl.await(1, TimeUnit.MINUTES);
		Startup.notifyReady();
	}

	@Override
	public void stop(BundleContext bc) throws Exception {
		synchronized (this) {
			if (serviceManager != null) {
				unregisterAddons();
				context.ungetService(serviceReference);
				serviceManager = null;
				serviceReference = null;
			}
		}
	}

	private void registerAddons() {
		if (serviceManager != null) {
			logger.info("register addons");
			System.err.println("register");
			serviceManager.registerServerComponentClass(BMSessionManager.class);

			serviceManager.registerPluginClass(MessageArchiver.class);
			serviceManager.registerPluginClass(BMMessageAll.class);
			serviceManager.registerPluginClass(BMPresence.class);
			serviceManager.registerPluginClass(BMOfflineMessages.class);

			serviceManager.registerClass(BMVHostsRepo.class);
			serviceManager.registerClass(BMUserRepo.class);
			serviceManager.registerClass(BMAuthRepo.class);
			serviceManager.registerClass(BMConfigurator.class);
			serviceManager.update();
		}
	}

	private void unregisterAddons() {
		if (serviceManager != null) {
			logger.info("unregister addons");
			System.err.println("unregister");
			serviceManager.unregisterServerComponentClass(BMSessionManager.class);

			serviceManager.unregisterPluginClass(MessageArchiver.class);
			serviceManager.unregisterPluginClass(BMMessageAll.class);
			serviceManager.unregisterPluginClass(BMPresence.class);
			serviceManager.unregisterPluginClass(BMOfflineMessages.class);

			serviceManager.unregisterClass(BMVHostsRepo.class);
			serviceManager.unregisterClass(BMUserRepo.class);
			serviceManager.unregisterClass(BMAuthRepo.class);
			serviceManager.update();
		}
	}
}
