package net.bluemind.configfile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.typesafe.config.Config;

import io.vertx.core.Vertx;

public class ReloadableConfig {

	public static final String RELOAD_ADDRESS = "system.signal.reload.config";

	private enum Key {
		INSTANCE
	}

	private final LoadingCache<Key, Config> cachedConfig;
	private final List<ConfigChangeListener> listeners = new ArrayList<>();

	public ReloadableConfig(Vertx vertx, Supplier<Config> configSupplier) {
		this.cachedConfig = Caffeine.newBuilder().build(key -> configSupplier.get());
		vertx.eventBus().consumer(RELOAD_ADDRESS, b -> {
			cachedConfig.invalidate(Key.INSTANCE);
			listeners.forEach(listener -> listener.onConfigChange(cachedConfig.get(Key.INSTANCE)));
		});

	}

	public Config config() {
		return cachedConfig.get(Key.INSTANCE);
	}

	public void addListener(ConfigChangeListener listener) {
		listeners.add(listener);
	}

}
