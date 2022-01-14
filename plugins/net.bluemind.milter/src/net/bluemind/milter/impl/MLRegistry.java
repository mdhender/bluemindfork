package net.bluemind.milter.impl;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.milter.IMilterListenerFactory;

public class MLRegistry {
	private static Logger logger = LoggerFactory.getLogger(MLRegistry.class);

	private static List<IMilterListenerFactory> loaded;

	static {
		init();
	}

	public final static Collection<IMilterListenerFactory> getFactories() {
		return loaded;
	}

	private static final void init() {
		logger.info("loading net.bluemind.milter.milterfactory extensions");
		RunnableExtensionLoader<IMilterListenerFactory> rel = new RunnableExtensionLoader<>();
		List<IMilterListenerFactory> tmp = rel.loadExtensions("net.bluemind.milter", "milterfactory", "milter_factory",
				"impl");
		logger.info("{} implementation found for extensionpoint net.bluemind.milter.milterfactory", tmp.size());
		loaded = ImmutableList.copyOf(tmp);
	}

}
