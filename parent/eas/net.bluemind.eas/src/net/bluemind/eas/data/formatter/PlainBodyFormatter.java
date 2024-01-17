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
package net.bluemind.eas.data.formatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.utils.HtmlToPlainText;

/**
 * Converts HTML mail body to text
 * 
 * 
 */
public final class PlainBodyFormatter {

	private static final Logger logger = LoggerFactory.getLogger(PlainBodyFormatter.class);

	private static final HtmlToPlainText extractor = new HtmlToPlainText();

	public PlainBodyFormatter() {
	}

	public String convert(String html) {
		if (html != null && !html.trim().isEmpty()) {
			try {
				return extractor.convert(html);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return "";
	}
}
