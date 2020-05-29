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
package net.bluemind.core.api;

import java.util.regex.Pattern;

public enum Regex {

	/**
	 * Blue Mind login
	 */
	LOGIN("^[a-z0-9][a-z0-9-._]{0,63}$"),

	/**
	 * Blue Mind mailshare name
	 */
	MAILSHARE_NAME("^[_a-z0-9][a-z0-9-._]{0,63}$"),

	/**
	 * local & domain part
	 */
	EMAIL("^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@([a-zA-Z0-9-]+\\.)+[a-z]{2,}$"),

	/**
	 * local part
	 */
	EMAIL_LEFT("^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*$"),

	/**
	 * Domain part
	 */
	DOMAIN("^([a-zA-Z0-9-]+\\.)+[a-z]{2,}$"),

	/**
	 * UUID regex
	 */
	UUID("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

	private final Pattern regexPattern;

	Regex(String regex) {
		this.regexPattern = Pattern.compile(regex);
	}

	public Pattern getRegexPattern() {
		return this.regexPattern;
	}

	public boolean validate(String value) {
		return this.regexPattern.matcher(value).matches();
	}
}
