package net.bluemind.systemcheck.checks;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;

public class CoreCountCheck extends AbstractCheck {

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults result, Map<String, String> collected)
			throws Exception {
		return checkCores();
	}

	private CheckResult checkCores() {
		CheckResult cr = ok("check.corecount");
		int procs = Runtime.getRuntime().availableProcessors();
		if (procs < 2) {
			cr = cr(CheckState.WARNING, "check.corecount", "reason.less.than.two.cores");
		}
		return cr;
	}

}
