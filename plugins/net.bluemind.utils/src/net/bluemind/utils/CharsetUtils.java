/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.utils;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharsetUtils {

	private static final Logger logger = LoggerFactory.getLogger(CharsetUtils.class);

	public static Charset forName(String charsetName) {
		Charset charset = StandardCharsets.UTF_8;
		try {
			charset = Charset.forName(charsetName);
		} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
			logger.info("Unsupported charset {}", charsetName);
		}
		return charset;
	}
}
