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
package net.bluemind.eas.dto.search;

public class GAL {

	public static final class Picture {

		public static enum Status {

			Success(1), NoPhoto(173), MaxSizeExceeded(174), //
			MaxPicturesExceeded(175);

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

	}

	private String displayName;
	public String phone;
	public String office;
	public String title;
	public String company;
	public String alias;
	public String firstname;
	public String lastname;
	public String homePhone;
	public String mobilePhone;
	public String emailAddress;
	public Picture picture;
	// TODO rm:RightsManagementLicense

	public void setDisplayName(String dn) {
		displayName = dn;
	}

	public String getDisplayName() {
		if (!isEmpty(displayName)) {
			return displayName;
		}

		if (!isEmpty(emailAddress)) {
			return emailAddress;
		}
		if (!isEmpty(mobilePhone)) {
			return mobilePhone;
		}

		if (!isEmpty(homePhone)) {
			return homePhone;
		}

		if (!isEmpty(phone)) {
			return phone;
		}

		return null;
	}

	private boolean isEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}

}
