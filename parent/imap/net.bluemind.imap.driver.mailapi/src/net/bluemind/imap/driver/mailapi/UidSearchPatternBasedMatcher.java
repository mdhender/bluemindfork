package net.bluemind.imap.driver.mailapi;

import java.util.regex.Pattern;

public abstract class UidSearchPatternBasedMatcher implements IUidSearchMatcher {
	protected final Pattern compiledRE;

	protected UidSearchPatternBasedMatcher(String regExp) {
		this.compiledRE = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE);
	}
}
