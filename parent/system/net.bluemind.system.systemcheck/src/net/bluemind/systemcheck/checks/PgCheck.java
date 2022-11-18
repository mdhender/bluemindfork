package net.bluemind.systemcheck.checks;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;

public class PgCheck extends AbstractCheck {

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults result, Map<String, String> collected)
			throws Exception {
		return checkPg(result, collected);
	}

	private CheckResult checkPg(SetupCheckResults checks, Map<String, String> customerData) {
		CheckResult cr = ok("check.pg");
		int ret = Integer.parseInt(customerData.get("check.pg"));
		if (ret != 0) {
			cr.setState(CheckState.ERROR);
		}
		return cr;
	}

}
