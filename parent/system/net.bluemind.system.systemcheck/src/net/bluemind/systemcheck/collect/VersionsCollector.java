package net.bluemind.systemcheck.collect;

import java.util.Map;

import net.bluemind.core.api.VersionInfo;
import net.bluemind.core.rest.IServiceProvider;

public class VersionsCollector implements IDataCollector {
	public void collect(IServiceProvider provider, Map<String, String> ret) throws Exception {
		VersionInfo swv = VersionHelper.getSWVersion();
		if (swv != null && swv.valid()) {
			ret.put("sw.version", swv.toString());
		}
	}

}
