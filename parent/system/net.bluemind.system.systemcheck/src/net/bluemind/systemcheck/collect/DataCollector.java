package net.bluemind.systemcheck.collect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.rest.IServiceProvider;

public class DataCollector {

	private DataCollector() {
	}

	private static final List<IDataCollector> collectors;

	static {
		collectors = ImmutableList.<IDataCollector>of(new MemoryCollector(), new VersionsCollector(),
				new CpusCollector(), new OSVersionCollector(), new LocalesCollector(), new NetworkCollector(),
				new PsqlWorkingCollector(), new UserStatsCollector(), new SubscriptionContactCollector());
	}

	private static final Logger logger = LoggerFactory.getLogger(DataCollector.class);

	public static Map<String, String> collectForUpgrade(IServiceProvider provider) {
		return collect(provider, true);
	}

	public static Map<String, String> collect(IServiceProvider provider) {
		return collect(provider, false);
	}

	public static Map<String, String> collect(IServiceProvider provider, boolean forupgrade) {
		Map<String, String> ret = new HashMap<>();

		for (IDataCollector cl : collectors) {
			if (forupgrade && !cl.collectForUpgrade()) {
				continue;
			}
			try {
				cl.collect(provider, ret);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

		for (Entry<String, String> s : ret.entrySet()) {
			logger.info("{} => {}", s.getKey(), s.getValue());
		}

		return ret;
	}

}
