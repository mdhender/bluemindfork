/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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

import org.apache.commons.lang3.StringUtils;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.role.api.BasicRoles;

public class DomainSettingsPasswordLifetimeValidator implements IValidator<DomainSettings> {

	public final static class Factory implements IValidatorFactory<DomainSettings> {
		@Override
		public Class<DomainSettings> support() {
			return DomainSettings.class;
		}

		@Override
		public IValidator<DomainSettings> create(BmContext context) {
			return new DomainSettingsPasswordLifetimeValidator(context);
		}
	}

	private BmContext context;

	public DomainSettingsPasswordLifetimeValidator(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(DomainSettings obj) throws ServerFault {
		// null is default value, allow it on create
		if (obj.settings.containsKey(DomainSettingsKeys.password_lifetime.name())
				&& obj.settings.get(DomainSettingsKeys.password_lifetime.name()) != null) {
			checkAccess(obj.domainUid);
			checkValue(obj.settings.get(DomainSettingsKeys.password_lifetime.name()));
		}
	}

	private void checkValue(String passwordLifetime) {
		if (passwordLifetime == null || passwordLifetime.isEmpty()) {
			return;
		}

		try {
			Integer pl = Integer.valueOf(passwordLifetime);
			if (pl < 1) {
				throw new ServerFault("Password lifetime must be greater or equals to 1.", ErrorCode.INVALID_PARAMETER);
			}
		} catch (NumberFormatException nfe) {
			throw new ServerFault(String.format("Invalid password lifetime '%s'.", passwordLifetime),
					ErrorCode.INVALID_PARAMETER);
		}
	}

	@Override
	public void update(DomainSettings oldValue, DomainSettings newValue) throws ServerFault {
		if (!StringUtils.equals(oldValue.settings.get(DomainSettingsKeys.password_lifetime.name()),
				newValue.settings.get(DomainSettingsKeys.password_lifetime.name()))) {
			checkAccess(newValue.domainUid);
			checkValue(newValue.settings.get(DomainSettingsKeys.password_lifetime.name()));
		}
	}

	private void checkAccess(String domainUid) {
		RBACManager.forContext(context).check(BasicRoles.ROLE_MANAGE_DOMAIN);
	}
}
