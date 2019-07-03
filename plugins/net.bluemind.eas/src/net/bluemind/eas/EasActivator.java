package net.bluemind.eas;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import net.bluemind.eas.command.provision.Policies;
import net.bluemind.eas.command.provision.WipedDevices;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.protocol.impl.Protocols;
import net.bluemind.eas.utils.DOMDumper;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.validation.IProtocolValidator;
import net.bluemind.eas.validation.Validator;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;

public class EasActivator implements BundleActivator {

	private static final Logger logger = LoggerFactory.getLogger(EasActivator.class);
	private static BundleContext context;
	private MQListener mqListener;

	public EasActivator() {
	}

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		EasActivator.context = bundleContext;
		IProtocolValidator validator = Validator.get();
		logger.debug("Validator: {}", validator);

		// to prevent crappy vertx classloader issues
		Document doc = DOMUtils.createDoc("bluemind", "Eas");
		DOMDumper.dumpXml(logger, "Startup:\n", doc);

		Backends.classLoad();
		WipedDevices.init();
		Policies.init();
		Protocols.registerProtocols();
		this.mqListener = new MQListener();
		MQ.init(new MQ.IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerConsumer(Topic.CORE_NOTIFICATIONS, mqListener);
				MQ.registerConsumer(Topic.HOOKS_DEVICE, mqListener);
			}
		});
	}

	public void stop(BundleContext bundleContext) throws Exception {
		EasActivator.context = null;
	}

}
