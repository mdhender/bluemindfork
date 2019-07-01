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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.tests.DontCallValidator.DontCall;

public class DontCallValidator implements IValidator<DontCall> {
	public class DontCall {
	}

	@SuppressWarnings("unused")
	private final BmContext context;

	/**
	 * @param context
	 */
	public DontCallValidator(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(DontCall obj) throws ServerFault {
		throw new ServerFault("you should not be here");
	}

	@Override
	public void update(DontCall previous, DontCall obj) throws ServerFault {
		throw new ServerFault("you should not be here");
	}
}
