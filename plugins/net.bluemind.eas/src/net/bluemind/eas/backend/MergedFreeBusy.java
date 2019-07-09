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
package net.bluemind.eas.backend;

public class MergedFreeBusy {

	public String to;
	public String displayName;
	public String email;
	public byte[] photo;

	public static enum SlotAvailability {

		Free(0), //
		Tentative(1), //
		Busy(2), //
		OutOfOffice(3), //
		NoData(4); //

		private int wireValue;

		private SlotAvailability(int wireValue) {
			this.wireValue = wireValue;
		}

		public String toString() {
			return "" + wireValue;
		}
	}

	public String mergeFreeBusy; // 1 digit for every 30min period

}
