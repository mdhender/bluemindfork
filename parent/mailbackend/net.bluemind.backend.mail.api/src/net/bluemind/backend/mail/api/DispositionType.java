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
package net.bluemind.backend.mail.api;

import net.bluemind.core.api.BMApi;

/** The disposition types of the "Content-Disposition" header (RFC-2183). */
@BMApi(version = "3")
public enum DispositionType {

	INLINE, ATTACHMENT;

	public static DispositionType valueOfNullSafeIgnoreCase(final String dispositionTypeString) {
		if (dispositionTypeString == null || dispositionTypeString.isEmpty()) {
			return null;
		}
		String fixedTypo = fixTypo(dispositionTypeString);

		return DispositionType.valueOf(fixedTypo);
	}

	private static String fixTypo(String dispositionTypeString) {
		String tmp = dispositionTypeString.toUpperCase();
		switch (tmp) {
		case "ATTACHEMENT":
			return DispositionType.ATTACHMENT.name();
		case "ENLIGNE":
			return DispositionType.INLINE.name();
		default:
			return tmp;
		}
	}
}
