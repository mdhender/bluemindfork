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
package net.bluemind.ui.gwtsharing.client;

public class ValidationResult {

	private boolean valid;
	
	public boolean isValid() {
		return valid;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	private String errorMessage;

	private ValidationResult(boolean isValid) {
		valid = isValid;
	}
	
	private ValidationResult(boolean isValid, String message) {
		valid = isValid;
		errorMessage = message;
	}

	public static ValidationResult valid() {
		return new ValidationResult(true);
	}
	
	public static ValidationResult invalid(String message) {
		return new ValidationResult(false, message);
	}
}
