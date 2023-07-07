package net.bluemind.configfile;

import com.typesafe.config.Config;

public interface ConfigChangeListener {

	void onConfigChange(Config config);

}
