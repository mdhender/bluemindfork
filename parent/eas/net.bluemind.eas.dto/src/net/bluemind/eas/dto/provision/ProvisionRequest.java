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
package net.bluemind.eas.dto.provision;

import net.bluemind.eas.dto.settings.SettingsRequest.DeviceInformation;

public class ProvisionRequest {

	public static final class Policies {
		public static final class Policy {
			public String policyType;
			public String policyKey;
			public String status;
		}

		public Policy policy = new Policy();
	}

	public static final class RemoteWipe {
		public static enum Status {

			Success(1), Error(2);

			private final String xmlValue;

			private Status(int value) {
				xmlValue = Integer.toString(value);
			}

			public String xmlValue() {
				return xmlValue;
			}

			public static Status get(String value) {
				switch (value) {
				case "0":
					return Success;
				case "2":
					return Error;
				}
				return null;
			}

		}

		public Status status;
	}

	public DeviceInformation deviceInformation;
	public Policies policies;
	public RemoteWipe remoteWipe;

}
