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
package net.bluemind.system.api;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SubscriptionInformations {

	@BMApi(version = "3")
	public static enum Kind {
		NONE(false), FREE(true), TRIAL(true), PROD(true), HOST(true);

		private final boolean validate;

		Kind(boolean validate) {
			this.validate = validate;
		}

		public boolean validate() {
			return validate;
		}
	}

	public String version;
	public String installationVersion;
	public String customer;
	public String customerCode;
	public String dealer;
	public String distributor;
	public Kind kind;
	public Date starts;
	public Date ends;
	public boolean valid;
	public String pubKeyFingerprint;
	public boolean validProvider = false;
	public String fromTechVersion;

	@BMApi(version = "3")
	public static class InstallationIndicator {
		@BMApi(version = "3")
		public static enum Kind {
			FullUser, SimpleUser
		}

		public Kind kind;
		public Integer maxValue;
		public Integer currentValue;
	}

	public List<InstallationIndicator> indicator = Collections.emptyList();
	public List<String> contacts = Collections.emptyList();

	@BMApi(version = "3")
	public static class Message {
		@BMApi(version = "3")
		public static enum Kind {
			Warning, Error
		}

		public Kind kind;

		@BMApi(version = "3")
		public static enum Code {
			Unknown, InvalidSignature, MaxAccounts, Expired
		}

		public Code code;

		public String message;
	}

	public List<Message> messages = Collections.emptyList();

	public boolean validProductiveLicense() {
		return kind != null && kind.validate() && valid;
	}

}
