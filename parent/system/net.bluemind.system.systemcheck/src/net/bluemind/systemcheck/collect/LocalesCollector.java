package net.bluemind.systemcheck.collect;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import net.bluemind.core.rest.IServiceProvider;

public class LocalesCollector implements IDataCollector {

	private static final Logger logger = LoggerFactory.getLogger(LocalesCollector.class);

	public void collect(IServiceProvider provider, Map<String, String> ret) throws Exception {
		try {
			CmdOutput cmdOut = SystemHelper.cmdWithEnv("/usr/bin/locale -a", (Map<String, String>) null);
			List<String> output = cmdOut.getOutput();
			String supportedLocales = Joiner.on(',').join(output);
			ret.put("supported.locales", supportedLocales);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
