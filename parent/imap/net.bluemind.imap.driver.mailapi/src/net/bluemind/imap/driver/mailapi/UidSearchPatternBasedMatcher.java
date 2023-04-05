package net.bluemind.imap.driver.mailapi;

import java.util.regex.Pattern;

public abstract class UidSearchPatternBasedMatcher implements IUidSearchMatcher {
	protected final Pattern compiledRE;

	protected UidSearchPatternBasedMatcher(Pattern re) {
		this.compiledRE = re;
	}
}
