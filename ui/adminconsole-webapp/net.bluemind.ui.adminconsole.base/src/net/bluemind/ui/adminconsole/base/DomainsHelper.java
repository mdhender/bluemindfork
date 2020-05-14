package net.bluemind.ui.adminconsole.base;

import net.bluemind.domain.api.Domain;

public class DomainsHelper {
	public static String getDisplayName(Domain domain) {
		if (domain.name.endsWith(".internal") && !domain.aliases.isEmpty()) {
			return domain.aliases.iterator().next();
		}

		return domain.name;
	}
}
