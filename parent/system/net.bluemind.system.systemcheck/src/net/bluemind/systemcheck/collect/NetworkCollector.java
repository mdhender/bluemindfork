package net.bluemind.systemcheck.collect;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;

public class NetworkCollector implements IDataCollector {
	public void collect(IServiceProvider provider, Map<String, String> ret) throws Exception {
		ret.put("net.ip", NetworkHelper.getMyIpAddress());
		java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
		String hName = localMachine.getCanonicalHostName();
		ret.put("net.hostname", hName);
	}
}
