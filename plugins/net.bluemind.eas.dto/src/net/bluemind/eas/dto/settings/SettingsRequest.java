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
package net.bluemind.eas.dto.settings;

import java.util.LinkedList;
import java.util.List;

public class SettingsRequest {

	public static final class Oof {

		public static final class Get {
			public String bodyType;
		}

		public static final class Set {

			public OofState state;
			public String startTime;
			public String endTime;

			/**
			 * size <= 3
			 */
			public List<OofMessage> oofMessages = new LinkedList<>();
		}

		public Get get;

	}

	public static final class DevicePassword {
	}

	public static final class DeviceInformation {

		public static final class Set {
			public String model;
			public String imei;
			public String friendlyName;
			public String os;
			public String osLanguage;
			public String phoneNumber;
			public String mobileOperator;
			public String userAgent;
		}

		public Set set;

	}

	public static final class UserInformation {
		public static final class Get {
		}

		public Get get;
	}

	public static final class RightsManagementInformation {
	}

	public Oof oof;

	public DevicePassword devicePassword;

	public DeviceInformation deviceInformation;

	public UserInformation userInformation;

	public RightsManagementInformation rightsManagementInformation;

}
