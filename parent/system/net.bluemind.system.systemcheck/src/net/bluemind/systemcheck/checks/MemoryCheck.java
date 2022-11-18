package net.bluemind.systemcheck.checks;

import java.util.Map;

import net.bluemind.core.rest.IServiceProvider;

public class MemoryCheck extends AbstractCheck {

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults result, Map<String, String> collected)
			throws Exception {
		return checkMemory(result, collected);
	}

	private CheckResult checkMemory(SetupCheckResults checks, Map<String, String> customerData) {
		CheckResult cr = ok("check.memory");
		String mem = customerData.get("mem.mb");
		if (mem == null) {
			cr = cr(CheckState.WARNING, "check.memory", "");
		} else {
			try {
				int mb = Integer.parseInt(mem);
				if (mb < 6000) {
					cr = cr(CheckState.WARNING, "check.memory", "reason.low.memory");
				}
			} catch (NumberFormatException nfe) {
				cr = cr(CheckState.WARNING, "check.memory", "");
			}
		}
		return cr;
	}

}
