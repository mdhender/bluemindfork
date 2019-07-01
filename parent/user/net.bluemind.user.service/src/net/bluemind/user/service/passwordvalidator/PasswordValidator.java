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
package net.bluemind.user.service.passwordvalidator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.user.hook.passwordvalidator.IPasswordValidatorFactory;

public class PasswordValidator {
	private static final Logger logger = LoggerFactory.getLogger(PasswordValidator.class);

	private BmContext context;
	private static final List<IPasswordValidatorFactory> validators = loadValidator();

	private static List<IPasswordValidatorFactory> loadValidator() {
		RunnableExtensionLoader<IPasswordValidatorFactory> rel = new RunnableExtensionLoader<IPasswordValidatorFactory>();
		List<IPasswordValidatorFactory> stores = rel.loadExtensions("net.bluemind.user.hook",
				"passwordvalidatorfactory", "passwordvalidatorfactory", "implementation");

		for (IPasswordValidatorFactory j : stores) {
			logger.info("password validator factory class: {}", j.getClass().getName());
		}

		return stores;
	}

	public PasswordValidator(BmContext context) {
		this.context = context;
	}

	public void validate(String password) throws ServerFault {
		for (IPasswordValidatorFactory passwordValidatorFactory : validators) {
			passwordValidatorFactory.create(context).validate(password);
		}
	}
}
