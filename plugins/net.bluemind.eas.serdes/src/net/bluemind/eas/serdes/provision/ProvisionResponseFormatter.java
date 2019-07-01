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

package net.bluemind.eas.serdes.provision;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.provision.ProvisionResponse;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class ProvisionResponseFormatter implements IEasResponseFormatter<ProvisionResponse> {
	@Override
	public void format(IResponseBuilder builder, double protocolVersion, ProvisionResponse response,
			Callback<Void> completion) {
		if (response == null) {
			completion.onResult(null);
			return;
		}

		builder.start(NamespaceMapping.Provision);

		if (protocolVersion > 14 && response.deviceInformation != null) {
			builder.container(NamespaceMapping.Settings, "DeviceInformation");
			builder.text("Status", "1");
			builder.endContainer();
		}

		builder.text(NamespaceMapping.Provision, "Status", response.status.xmlValue());

		if (response.policies != null) {
			builder.container("Policies").container("Policy");
			builder.text("PolicyType", response.policies.policy.policyType);
			builder.text("Status", response.policies.policy.status.xmlValue());
			if (response.policies.policy.policyKey != null) {
				builder.text("PolicyKey", response.policies.policy.policyKey);
			}

			if (response.policies.policy.data != null) {
				builder.container("Data").container("EASProvisionDoc");

				builder.text("DevicePasswordEnabled", response.policies.policy.data.devicePasswordEnabled ? "1" : "0");
				builder.text("AlphanumericDevicePasswordRequired",
						response.policies.policy.data.alphanumericDevicePasswordRequired ? "1" : "0");
				builder.text("PasswordRecoveryEnabled",
						response.policies.policy.data.passwordRecoveryEnabled ? "1" : "0");
				builder.text("RequireStorageCardEncryption",
						response.policies.policy.data.requireStorageCardEncryption ? "1" : "0");
				builder.text("AttachmentsEnabled", response.policies.policy.data.attachmentsEnabled ? "1" : "0");
				builder.text("MinDevicePasswordLength", response.policies.policy.data.minDevicePasswordLength + "");
				builder.text("MaxInactivityTimeDeviceLock",
						response.policies.policy.data.maxInactivityTimeDeviceLock + "");
				builder.text("MaxDevicePasswordFailedAttempts",
						response.policies.policy.data.maxDevicePasswordFailedAttempts + "");
				builder.text("MaxAttachmentSize", response.policies.policy.data.maxAttachmentSize + "");
				builder.text("AllowSimpleDevicePassword",
						response.policies.policy.data.allowSimpleDevicePassword ? "1" : "0");
				builder.token("DevicePasswordExpiration");
				builder.text("DevicePasswordHistory", response.policies.policy.data.devicePasswordHistory + "");

				if (protocolVersion > 12.0) {
					builder.text("AllowStorageCard", response.policies.policy.data.allowStorageCard ? "1" : "0");
					builder.text("AllowCamera", response.policies.policy.data.allowCamera ? "1" : "0");
					builder.text("RequireDeviceEncryption",
							response.policies.policy.data.requireDeviceEncryption ? "1" : "0");
					builder.text("AllowUnsignedApplications",
							response.policies.policy.data.allowUnsignedApplications ? "1" : "0");
					builder.text("AllowUnsignedInstallationPackages",
							response.policies.policy.data.allowUnsignedInstallationPackages ? "1" : "0");
					builder.text("MinDevicePasswordComplexCharacters",
							response.policies.policy.data.minDevicePasswordComplexCharacters + "");
					builder.text("AllowWiFi", response.policies.policy.data.allowWiFi ? "1" : "0");
					builder.text("AllowTextMessaging", response.policies.policy.data.allowTextMessaging ? "1" : "0");
					builder.text("AllowPOPIMAPEmail", response.policies.policy.data.allowPOPIMAPEmail ? "1" : "0");
					builder.text("AllowBluetooth", response.policies.policy.data.allowBluetooth + "");
					builder.text("AllowIrDA", response.policies.policy.data.allowIrDA ? "1" : "0");
					builder.text("RequireManualSyncWhenRoaming",
							response.policies.policy.data.requireManualSyncWhenRoaming ? "1" : "0");
					builder.text("AllowDesktopSync", response.policies.policy.data.allowDesktopSync ? "1" : "0");
					builder.text("MaxCalendarAgeFilter", response.policies.policy.data.maxCalendarAgeFilter + "");
					builder.text("AllowHTMLEmail", response.policies.policy.data.allowHTMLEmail ? "1" : "0");
					builder.text("MaxEmailAgeFilter", response.policies.policy.data.maxEmailAgeFilter + "");
					builder.text("MaxEmailBodyTruncationSize",
							response.policies.policy.data.maxEmailBodyTruncationSize + "");
					builder.text("MaxEmailHTMLBodyTruncationSize",
							response.policies.policy.data.maxEmailHTMLBodyTruncationSize + "");
					builder.text("RequireSignedSMIMEMessages",
							response.policies.policy.data.requireSignedSMIMEMessages ? "1" : "0");
					builder.text("RequireEncryptedSMIMEMessages",
							response.policies.policy.data.requireEncryptedSMIMEMessages + "");
					builder.text("RequireSignedSMIMEAlgorithm",
							response.policies.policy.data.requireSignedSMIMEAlgorithm + "");
					builder.text("RequireEncryptionSMIMEAlgorithm",
							response.policies.policy.data.requireEncryptionSMIMEAlgorithm + "");
					builder.text("AllowSMIMEEncryptionAlgorithmNegotiation",
							response.policies.policy.data.allowSMIMEEncryptionAlgorithmNegotiation + "");
					builder.text("AllowSMIMESoftCerts", response.policies.policy.data.allowSMIMESoftCerts ? "1" : "0");
					builder.text("AllowBrowser", response.policies.policy.data.allowBrowser ? "1" : "0");
					builder.text("AllowConsumerEmail", response.policies.policy.data.allowConsumerEmail ? "1" : "0");
					builder.text("AllowRemoteDesktop", response.policies.policy.data.allowRemoteDesktop ? "1" : "0");
					builder.text("AllowInternetSharing",
							response.policies.policy.data.allowInternetSharing ? "1" : "0");

					builder.token("UnapprovedInROMApplicationList");// FIXME
					builder.token("ApprovedApplicationList"); // FIXME
				}
				builder.endContainer().endContainer();
			}
			builder.endContainer().endContainer();
		}

		if (response.remoteWipe != null) {
			builder.token("RemoteWipe");
		}
		builder.end(completion);
	}

}
