package net.bluemind.systemcheck.collect;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.IServiceProvider;

public class PsqlWorkingCollector implements IDataCollector {

	private static final Logger logger = LoggerFactory.getLogger(PsqlWorkingCollector.class);

	protected static String SQL_PATH = "/usr/share/bm-setup-wizard/tpl";

	static {
		File dir = new File("/usr/share/bm-installation-wizard/tpl");
		if (dir.exists() && dir.isDirectory()) {
			SQL_PATH = dir.getAbsolutePath();
		}
	}

	public void collect(IServiceProvider provider, Map<String, String> collected) throws Exception {
		try {
			int check = checkPg();
			collected.put("check.pg", "" + check);
		} catch (IOException e) {
			logger.error("pg check failed");
			collected.put("check.pg", "1");
		}
	}

	private int checkPg() throws IOException {
		CmdOutput out = SystemHelper.cmdWithEnv(SQL_PATH + "/check_pg.sh", (Map<String, String>) null);
		return out.getExitCode();
	}
}
