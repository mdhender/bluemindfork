package net.bluemind.systemcheck.checks;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;

public class NetworkCheck extends AbstractCheck {

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults results, Map<String, String> collected)
			throws Exception {
		CheckResult cr = ok("check.hostname");
		String host = collected.get("net.hostname");
		if (host == null) {
			cr = cr(CheckState.WARNING, "check.hostname", "hostname -f gave an incorrect result");
		} else if (!host.contains(".")) {
			cr = cr(CheckState.WARNING, "check.hostname", "hostname -f is not fully qualified");
		}
		return cr;
	}

}
