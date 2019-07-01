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

import java.util.List;

import net.bluemind.eas.dto.settings.SettingsResponse.DeviceInformation;

public class ProvisionResponse {

	public static enum Status {

		Success(1), ProtocolError(2), ServerError(3);

		private final String xmlValue;

		private Status(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public static final class Policies {

		public static final class Policy {

			public static enum Status {

				Success(1), NoPolicy(2), Unknown(3), Corrupted(4), Wrong(5);

				private final String xmlValue;

				private Status(int value) {
					xmlValue = Integer.toString(value);
				}

				public String xmlValue() {
					return xmlValue;
				}

			}

			public static final class EASProvisionDoc {
				public int allowBluetooth = 2;
				public boolean allowBrowser = true;
				public boolean allowCamera = true;
				public boolean allowConsumerEmail = true;
				public boolean allowDesktopSync = true;
				public boolean allowHTMLEmail = true;
				public boolean allowInternetSharing = true;
				public boolean allowIrDA = true;
				public boolean allowPOPIMAPEmail = true;
				public boolean allowRemoteDesktop = true;
				public boolean allowSimpleDevicePassword = true;
				public int allowSMIMEEncryptionAlgorithmNegotiation = 2;
				public boolean allowSMIMESoftCerts = true;
				public boolean allowStorageCard = true;
				public boolean allowTextMessaging = true;
				public boolean allowUnsignedApplications = true;
				public boolean allowUnsignedInstallationPackages = true;
				public boolean allowWiFi = true;
				public boolean alphanumericDevicePasswordRequired = false;
				public List<String> approvedApplicationList = null;
				public boolean attachmentsEnabled = true;
				public boolean devicePasswordEnabled = false;
				public int devicePasswordExpiration = 0;
				public int devicePasswordHistory = 0;
				public int maxAttachmentSize = 10 * 1024 * 1024;
				public int maxCalendarAgeFilter = 0;
				public int maxDevicePasswordFailedAttempts = 8;
				public int maxEmailAgeFilter = 0;
				public int maxEmailBodyTruncationSize = -1;
				public int maxEmailHTMLBodyTruncationSize = -1;
				public int maxInactivityTimeDeviceLock = 900;
				public int minDevicePasswordComplexCharacters = 3;
				public int minDevicePasswordLength = 4;
				public boolean passwordRecoveryEnabled = false;
				public boolean requireDeviceEncryption = false;
				public int requireEncryptedSMIMEMessages = 0;
				public int requireEncryptionSMIMEAlgorithm = 0;
				public boolean requireManualSyncWhenRoaming = false;
				public int requireSignedSMIMEAlgorithm = 0;
				public boolean requireSignedSMIMEMessages = false;
				public boolean requireStorageCardEncryption = false;
				public List<String> unapprovedInROMApplicationList = null;
			}

			public String policyType;
			public String policyKey;
			public Status status;
			public EASProvisionDoc data;
		}

		public Policy policy = new Policy();
	}

	public static final class RemoteWipe {
	}

	public DeviceInformation deviceInformation;
	public Policies policies;
	public RemoteWipe remoteWipe;
	public Status status;
}
