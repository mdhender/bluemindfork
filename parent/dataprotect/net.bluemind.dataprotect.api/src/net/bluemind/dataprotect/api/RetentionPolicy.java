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
package net.bluemind.dataprotect.api;

import net.bluemind.core.api.BMApi;

/**
 * This class stores informations for {@link DataProtectGeneration} retention
 * and automatic deletion.
 */
@BMApi(version = "3")
public class RetentionPolicy {

	/**
	 * Returns how many daily backup we should keep
	 * 
	 * <p>
	 * null when we don't apply a daily retention policy
	 * </p>
	 */
	public Integer daily = 1;
	/**
	 * Returns how many weekly backup we should keep
	 * 
	 * <p>
	 * null when we don't apply a weekly retention policy
	 * </p>
	 */
	@Deprecated
	public Integer weekly;
	/**
	 * Returns how many monthly backup we should keep
	 * 
	 * <p>
	 * null when we don't apply a monthly retention policy
	 * </p>
	 */
	@Deprecated
	public Integer monthly;

}
