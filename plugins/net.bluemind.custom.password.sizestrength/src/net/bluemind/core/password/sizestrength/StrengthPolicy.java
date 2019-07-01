/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.password.sizestrength;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class StrengthPolicy {
	private static final String CONF_FILE = "/etc/bm/password.ini";
	private static final int DEFAULT_MINIMUM_LENGTH = 6;
	private static final int DEFAULT_MINIMUM_DIGIT = 1;
	private static final int DEFAULT_MINIMUM_CAPITAL = 1;
	private static final int DEFAULT_MINIMUM_LOWER = 1;
	private static final int DEFAULT_MINIMUM_PUNCT = 1;

	public final int minimumLength;
	public final int minimumDigit;
	public final int minimumCapital;
	public final int minimumLower;
	public final int minimumPunct;

	public static StrengthPolicy build() {
		Properties p = new Properties();
		try {
			FileInputStream fis = new FileInputStream(CONF_FILE);
			p.load(fis);
		} catch (IOException e) {
			return new StrengthPolicy();
		}

		Integer minimumDigit = null;
		try {
			minimumDigit = Integer.parseInt(p.getProperty("digit"));
		} catch (NumberFormatException nfe) {
			minimumDigit = DEFAULT_MINIMUM_DIGIT;
		}

		Integer minimumCapital = null;
		try {
			minimumCapital = Integer.parseInt(p.getProperty("capital"));
		} catch (NumberFormatException nfe) {
			minimumCapital = DEFAULT_MINIMUM_CAPITAL;
		}

		Integer minimumLower = null;
		try {
			minimumLower = Integer.parseInt(p.getProperty("lower"));
		} catch (NumberFormatException nfe) {
			minimumLower = DEFAULT_MINIMUM_LOWER;
		}

		Integer minimumPunct = null;
		try {
			minimumPunct = Integer.parseInt(p.getProperty("special"));
		} catch (NumberFormatException nfe) {
			minimumPunct = DEFAULT_MINIMUM_PUNCT;
		}

		Integer minimumLength = null;
		try {
			minimumLength = Integer.parseInt(p.getProperty("length"));
		} catch (NumberFormatException nfe) {
			minimumLength = DEFAULT_MINIMUM_LENGTH;
		}

		if (minimumLength < minimumDigit + minimumCapital + minimumLower + minimumPunct) {
			minimumLength = minimumDigit + minimumCapital + minimumLower + minimumPunct;
		}

		return new StrengthPolicy(minimumLength, minimumPunct, minimumLower, minimumCapital, minimumDigit);
	}

	private StrengthPolicy(Integer minimumLength, Integer minimumPunct, Integer minimumLower, Integer minimumCapital,
			Integer minimumDigit) {
		this.minimumLength = minimumLength;
		this.minimumPunct = minimumPunct;
		this.minimumLower = minimumLower;
		this.minimumCapital = minimumCapital;
		this.minimumDigit = minimumDigit;
	}

	private StrengthPolicy() {
		this.minimumLength = DEFAULT_MINIMUM_LENGTH;
		this.minimumPunct = DEFAULT_MINIMUM_PUNCT;
		this.minimumLower = DEFAULT_MINIMUM_LOWER;
		this.minimumCapital = DEFAULT_MINIMUM_CAPITAL;
		this.minimumDigit = DEFAULT_MINIMUM_DIGIT;
	}
}
