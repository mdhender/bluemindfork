package net.bluemind.systemcheck.checks;

import java.util.Map;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.system.api.InstallationVersion;

public abstract class AbstractCheck {

	public AbstractCheck() {

	}

	public boolean canCheckWithVersion(InstallationVersion version) {
		return true;
	}

	public abstract CheckResult verify(IServiceProvider provider, SetupCheckResults results,
			Map<String, String> collected) throws Exception;

	protected CheckResult cr(CheckState warning, String titleKey, String reasonKey) {
		CheckResult cr = new CheckResult();
		cr.setState(warning);
		cr.setTitleKey(titleKey);
		cr.setReasonKey(reasonKey);
		return cr;
	}

	protected CheckResult ok(String check) {
		CheckResult cr = new CheckResult();
		cr.setState(CheckState.OK);
		cr.setTitleKey(check);
		return cr;
	}

	protected CheckResult error(String check) {
		CheckResult cr = new CheckResult();
		cr.setState(CheckState.ERROR);
		cr.setTitleKey(check);
		return cr;
	}

}
