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
package net.bluemind.core.password.sizestrength;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.hook.passwordvalidator.IPasswordValidator;

public class SizeStrengthPolicy implements IPasswordValidator {
	private BmContext context;
	private final StrengthPolicy strengthPolicy;

	public SizeStrengthPolicy(BmContext context, StrengthPolicy strengthPolicy) {
		this.context = context;
		this.strengthPolicy = strengthPolicy;
	}

	@Override
	public void validate(String password) throws ServerFault {
		if (context.getSecurityContext().isDomainGlobal()) {
			return;
		}

		if (password.length() < strengthPolicy.minimumLength) {
			throw new ServerFault(getErrorMessage(), ErrorCode.INVALID_PASSWORD);
		}

		checkCapital(password);
		checkLower(password);
		checkDigit(password);
		checkPunct(password);
	}

	private void checkPunct(String password) throws ServerFault {
		if (!Pattern.matches("(.*\\p{Punct}){" + strengthPolicy.minimumPunct + ",}.*", password)) {
			throw new ServerFault(getErrorMessage(), ErrorCode.INVALID_PASSWORD);
		}
	}

	private void checkLower(String password) throws ServerFault {
		if (!Pattern.matches("(.*\\p{Lower}){" + strengthPolicy.minimumLower + ",}.*", password)) {
			throw new ServerFault(getErrorMessage(), ErrorCode.INVALID_PASSWORD);
		}
	}

	private void checkCapital(String password) throws ServerFault {
		if (!Pattern.matches("(.*\\p{Upper}){" + strengthPolicy.minimumCapital + ",}.*", password)) {
			throw new ServerFault(getErrorMessage(), ErrorCode.INVALID_PASSWORD);
		}
	}

	private void checkDigit(String password) throws ServerFault {
		if (!Pattern.matches("(.*\\p{Digit}){" + strengthPolicy.minimumDigit + ",}.*", password)) {
			throw new ServerFault(getErrorMessage(), ErrorCode.INVALID_PASSWORD);
		}
	}

	private String getErrorMessage() throws ServerFault {
		ResourceBundle myResources = ResourceBundle.getBundle("SizeStrengthPolicy",
				new Locale(context.getSecurityContext().getLang()));

		Object[] passwordSyntax = { strengthPolicy.minimumLength, strengthPolicy.minimumCapital,
				strengthPolicy.minimumLower, strengthPolicy.minimumDigit, strengthPolicy.minimumPunct };
		return new MessageFormat(myResources.getString("invalidsyntax")).format(passwordSyntax);
	}
}
