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
package net.bluemind.imap;

public class Annotation {
	public final String valueShared;
	public final String valuePriv;

	private Annotation(String valueShared, String valuePriv) {
		this.valueShared = valueShared;
		this.valuePriv = valuePriv;
	}

	public static Annotation fromSharedValue(String valueShared) {
		return new Annotation(valueShared, null);
	}

	public static Annotation fromPrivValue(String valuePriv) {
		return new Annotation(null, valuePriv);
	}

	public static Annotation merge(Annotation orig, Annotation valueShared, Annotation valuePriv) {
		if (orig == null) {
			return new Annotation(valueShared != null ? valueShared.valueShared : null,
					valuePriv != null ? valuePriv.valuePriv : null);
		}

		if (valueShared != null && valuePriv != null) {
			return new Annotation(valueShared.valueShared, valuePriv.valuePriv);
		} else if (valueShared != null) {
			return new Annotation(valueShared.valueShared, orig.valuePriv);
		} else if (valuePriv != null) {
			return new Annotation(orig.valueShared, valuePriv.valuePriv);
		}

		return orig;
	}
}
