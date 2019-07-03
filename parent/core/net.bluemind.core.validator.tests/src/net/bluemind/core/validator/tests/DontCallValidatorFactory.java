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
package net.bluemind.core.validator.tests;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.core.validator.tests.DontCallValidator.DontCall;

public class DontCallValidatorFactory implements IValidatorFactory<DontCall> {
	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.validator.IValidatorFactory#support()
	 */
	@Override
	public Class<DontCall> support() {
		return DontCall.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.bluemind.core.validator.IValidatorFactory#create(net.bluemind.core
	 * .rest.BmContext)
	 */
	@Override
	public IValidator<DontCall> create(BmContext context) {
		return new DontCallValidator(context);
	}
}
