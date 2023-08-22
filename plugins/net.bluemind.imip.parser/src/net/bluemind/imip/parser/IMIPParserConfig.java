/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imip.parser;

public class IMIPParserConfig {

	public final boolean failOnMissingMethod;

	private IMIPParserConfig(boolean failOnMissingMethod) {
		this.failOnMissingMethod = failOnMissingMethod;
	}

	public static IMIPParserConfig defaultConfig() {
		return new IMIPParserConfigBuilder().failOnMissingMethod(true).create();
	}

	public static class IMIPParserConfigBuilder {

		private boolean failOnMissingMethod = true;

		public IMIPParserConfigBuilder failOnMissingMethod(boolean failOnMissingMethod) {
			this.failOnMissingMethod = failOnMissingMethod;
			return this;
		}

		public IMIPParserConfig create() {
			return new IMIPParserConfig(failOnMissingMethod);
		}
	}

}
