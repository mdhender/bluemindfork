package net.bluemind.systemcheck.checks;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;

public class EnUSLocaleCheck extends AbstractCheck {

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults results, Map<String, String> collected)
			throws Exception {
		CheckResult cr = ok("check.en_us");
		String supported = collected.get("supported.locales");
		if (supported == null || (!supported.contains("en_US.utf8") && !supported.contains("en_US.UTF-8"))) {
			cr = cr(CheckState.ERROR, "check.en_us", "reason.missing.locale");
		}
		return cr;
	}

}
