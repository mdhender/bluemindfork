package net.bluemind.sds.store.scalityring;

import java.io.File;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ScalityHttpClientConfig {
	private static final Config INSTANCE = loadConfig();
	private static final String CONFIG_PATH = "/etc/bm/scality.conf";

	private ScalityHttpClientConfig() {

	}

	private static Config loadConfig() {
		Config referenceConfig = ConfigFactory.parseResourcesAnySyntax(ScalityHttpClientConfig.class.getClassLoader(),
				"resources/scality.conf");
		Config resolvedConfig = referenceConfig;
		File local = new File(CONFIG_PATH);
		if (local.exists()) {
			resolvedConfig = ConfigFactory.parseFile(local);
			resolvedConfig = resolvedConfig.withFallback(referenceConfig).resolve();
		}
		return resolvedConfig.withOnlyPath("scality");
	}

	public static Config get() {
		return INSTANCE;
	}
}
