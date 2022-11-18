package net.bluemind.systemcheck.collect;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.system.helper.distrib.OsVersionDetectionFactory;
import net.bluemind.system.helper.distrib.list.Distribution;

public class OSVersionCollector implements IDataCollector {
	public void collect(IServiceProvider provider, Map<String, String> ret) throws Exception {
		Distribution distrib = OsVersionDetectionFactory.create().detect();
		ret.put("os", distrib.getName());
	}
}
