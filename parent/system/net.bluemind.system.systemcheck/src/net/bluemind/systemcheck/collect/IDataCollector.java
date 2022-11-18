package net.bluemind.systemcheck.collect;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;

public interface IDataCollector {
	public abstract void collect(IServiceProvider provider, Map<String, String> collected) throws Exception;

	public default boolean collectForUpgrade() {
		return true;
	}
}
