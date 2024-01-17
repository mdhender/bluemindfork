/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2014
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.eas.data.formatter;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert text/plain mail body to HTML. The HTML version should be used for
 * display & rich formatting.
 * 
 * 
 */
public class HTMLBodyFormatter {

	private static final Logger logger = LoggerFactory.getLogger(HTMLBodyFormatter.class);

	private static final Pattern urlPattern = Pattern.compile(
			"(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|])");

	public HTMLBodyFormatter() {
	}

	public String convert(String plain) {

		StringBuilder sb = new StringBuilder(plain.length() * 2);
		sb.append("<html><body>");
		String escaped = simpleEscape(plain);
		escaped = escaped.replace("\r\n", "\n");
		escaped = escaped.replace("\n", "<br>\n");
		escaped = urlPattern.matcher(escaped).replaceAll("<a href=\"$1\">$1</a>");
		escaped = escaped.replace("<a href=\"www", "<a href=\"http://www");
		sb.append(escaped);
		sb.append("</body></html>");

		String ret = sb.toString();
		if (logger.isDebugEnabled()) {
			logger.debug("Converted result:\n" + ret);
		}
		return ret;

	}

	private String simpleEscape(String plain) {
		String rep = plain;

		rep = rep.replace("<", "&lt;");
		rep = rep.replace(">", "&gt;");

		// we don't use that as HTC one X does not know how to render html
		// entities
		// rep = StringEscapeUtils.escapeHtml(plain);

		return rep;
	}

}
