/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.domain.service.internal;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.role.api.BasicRoles;

public class DomainSettingsMaxBasicAccountValidator implements IValidator<DomainSettings> {

	public final static class Factory implements IValidatorFactory<DomainSettings> {

		@Override
		public Class<DomainSettings> support() {
			return DomainSettings.class;
		}

		@Override
		public IValidator<DomainSettings> create(BmContext context) {
			return new DomainSettingsMaxBasicAccountValidator(context);
		}

	}

	private BmContext context;

	public DomainSettingsMaxBasicAccountValidator(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(DomainSettings settings) throws ServerFault {
		// null is default value, allow it on create
		if (settings.settings.containsKey(DomainSettingsKeys.domain_max_basic_account.name())
				&& settings.settings.get(DomainSettingsKeys.domain_max_basic_account.name()) != null) {
			RBACManager.forContext(context).check(BasicRoles.ROLE_DOMAIN_MAX_VALUES);
			checkValue(settings.settings);
		}
	}

	@Override
	public void update(DomainSettings oldValue, DomainSettings newValue) throws ServerFault {
		if (!StringUtils.equals(oldValue.settings.get(DomainSettingsKeys.domain_max_basic_account.name()),
				newValue.settings.get(DomainSettingsKeys.domain_max_basic_account.name()))) {
			RBACManager.forContext(context).check(BasicRoles.ROLE_DOMAIN_MAX_VALUES);

			checkValue(newValue.settings);
		}
	}

	private void checkValue(Map<String, String> settings) throws ServerFault {
		String setting = settings.get(DomainSettingsKeys.domain_max_basic_account.name());
		if (setting == null || setting.isEmpty()) {
			return;
		}

		try {
			Integer.parseInt(setting);
		} catch (NumberFormatException nfe) {
			throw new ServerFault("Invalid maximum number of basic account.", ErrorCode.INVALID_PARAMETER);
		}
	}

}
