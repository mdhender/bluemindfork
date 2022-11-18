package net.bluemind.systemcheck.collect;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;

public class CpusCollector implements IDataCollector {
	public void collect(IServiceProvider provider, Map<String, String> collected) throws Exception {
		collected.put("cpu.cores", "" + Runtime.getRuntime().availableProcessors());
	}
}
