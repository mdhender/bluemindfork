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
package net.bluemind.eas.serdes.base;

import org.w3c.dom.Element;

import net.bluemind.eas.dto.itemoperations.ItemOperationsRequest.Fetch.Options.Range;

public class RangeParser {

	public Range parse(Element rangeElt) {
		String asTxt = rangeElt.getTextContent();
		return parse(asTxt);

	}

	private Range parse(String asTxt) {
		Range ret = new Range();
		int splitIndex = asTxt.indexOf('-');
		if (splitIndex < 0) {
			return null;
		}
		ret.start = Integer.parseInt(asTxt.substring(0, splitIndex));
		ret.end = Integer.parseInt(asTxt.substring(splitIndex + 1));
		return ret;
	}

}
