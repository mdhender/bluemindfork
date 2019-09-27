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
package net.bluemind.system.validation;

public interface IProductValidator {

	public String getName();

	public ValidationResult validate();

	public static class ValidationResult {
		public final boolean valid;
		public final String message;

		private ValidationResult(boolean valid, String message) {
			this.valid = valid;
			this.message = message;
		}

		public static ValidationResult valid(String message) {
			return new ValidationResult(true, message);
		}

		public static ValidationResult valid() {
			return valid("OK");
		}

		public static ValidationResult notValid(String message) {
			return new ValidationResult(false, message);
		}
	}

}
