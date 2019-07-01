/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.filehosting.config;

import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.IGlobalSettings;

public class FileHostingSettingsValidator implements IValidator<DomainSettings> {

	public static class FileHostingSettingsValidatorFactory implements IValidatorFactory<DomainSettings> {

		@Override
		public Class<DomainSettings> support() {
			return DomainSettings.class;
		}

		@Override
		public IValidator<DomainSettings> create(BmContext context) {
			return new FileHostingSettingsValidator();
		}

	}

	public FileHostingSettingsValidator() {

	}

	@Override
	public void create(DomainSettings settings) throws ServerFault {
		validateMaxFilesize(settings);

	}

	@Override
	public void update(DomainSettings oldValue, DomainSettings newValue) throws ServerFault {
		validateMaxFilesize(newValue);
	}

	private void validateMaxFilesize(DomainSettings domainSettings) {
		Map<String, String> settings = domainSettings.settings;
		String maxSizeKey = GlobalSettingsKeys.filehosting_max_filesize.name();

		if (settings.containsKey(maxSizeKey)) {
			Map<String, String> sysConfig = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IGlobalSettings.class).get();
			if (sysConfig.containsKey(maxSizeKey)) {
				long domainSetting = Long.parseLong(settings.get(maxSizeKey));
				long systemSetting = Long.parseLong(sysConfig.get(maxSizeKey));
				if (domainSetting > 0 && systemSetting > 0) {
					if (systemSetting < domainSetting) {
						throw new ServerFault(
								"Domain specific value for Filehosting (Max File size) cannot be greater than system value "
										+ normalize(systemSetting));
					}
				}
			}
		}

	}

	private long normalize(long setting) {
		return setting / 1024 / 1024;
	}
}
