/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.eas.dto.base;

public class Picture {

	public enum Status {

		SUCCESS(1), NO_PHOTO(173), MAX_SIZE_EXCEEDED(174), //
		MAX_PICTURES_EXCEEDED(175);

		private final String xmlValue;

		private Status(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public Status status;
	public String data;

	public static Picture noPhoto() {
		Picture picture = new Picture();
		picture.status = Picture.Status.NO_PHOTO;
		return picture;
	}
}
