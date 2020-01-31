package net.bluemind.user.service.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.bluemind.domain.api.IDomainSettings;

class UserSettingsSanitizer {

	public void sanitize(Map<String, String> settings, IDomainSettings domainSettingsService) {
		Map<String, String> domainSettings = domainSettingsService.get();
		List<String> duplicateKeys = new ArrayList<>();
		
		settings.keySet().forEach(key -> {
			if (domainSettings.containsKey(key) && settingsEquals(domainSettings.get(key), settings.get(key))) {
				duplicateKeys.add(key);
			}
		});

		duplicateKeys.forEach(key -> settings.remove(key));
	}

	private boolean settingsEquals(String value1, String value2) {
		return value1 == null ? value2 == null : value1.equals(value2);
	}

}