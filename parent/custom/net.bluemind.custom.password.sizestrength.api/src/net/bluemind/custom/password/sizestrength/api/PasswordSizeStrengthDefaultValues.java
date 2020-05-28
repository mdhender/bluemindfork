/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.custom.password.sizestrength.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class PasswordSizeStrengthDefaultValues {
	public static final int DEFAULT_MINIMUM_LENGTH = 6;
	public static final int DEFAULT_MINIMUM_DIGIT = 1;
	public static final int DEFAULT_MINIMUM_CAPITAL = 1;
	public static final int DEFAULT_MINIMUM_LOWER = 1;
	public static final int DEFAULT_MINIMUM_PUNCT = 1;
}
