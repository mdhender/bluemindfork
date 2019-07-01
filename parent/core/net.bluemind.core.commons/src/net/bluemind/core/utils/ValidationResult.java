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
package net.bluemind.core.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ValidationResult {

	public final boolean valid;

	public final Map<String, Boolean> validationResults;

	public ValidationResult(boolean valid, Map<String, Boolean> validationResults) {
		this.valid = valid;
		this.validationResults = Collections.unmodifiableMap(validationResults);
	}

	public ValidationResult(boolean valid, String[] uids) {
		Map<String, Boolean> results = new HashMap<>();
		for (String uid : uids) {
			results.put(uid, valid);
		}
		this.valid = valid;
		this.validationResults = Collections.unmodifiableMap(results);
	}

}
