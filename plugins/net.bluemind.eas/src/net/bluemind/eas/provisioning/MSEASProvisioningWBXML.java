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
package net.bluemind.eas.provisioning;

import org.w3c.dom.Element;

import net.bluemind.eas.utils.DOMUtils;

/**
 * Policy type for protocol 12.x (windows mobile 6, iPhone, etc)
 * 
 * 
 */

public class MSEASProvisioningWBXML extends Policy {

	private double protocolVersion;

	public MSEASProvisioningWBXML(double protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	private void p(Element provDoc, String field, String value) {
		DOMUtils.createElementAndText(provDoc, field, value);
	}

	@Override
	public void serialize(Element data) {

		Element provDoc = DOMUtils.createElement(data, "EASProvisionDoc");

		p(provDoc, "DevicePasswordEnabled", "0");
		p(provDoc, "AlphanumericDevicePasswordRequired", "0");

		p(provDoc, "PasswordRecoveryEnabled", "0");
		p(provDoc, "RequireStorageCardEncryption", "0");
		p(provDoc, "AttachmentsEnabled", "1");
		p(provDoc, "MinDevicePasswordLength", "4");

		p(provDoc, "MaxInactivityTimeDeviceLock", "900");
		p(provDoc, "MaxDevicePasswordFailedAttempts", "8");
		DOMUtils.createElementAndText(provDoc, "MaxAttachmentSize", "" + 10 * 1024 * 1024);

		p(provDoc, "AllowSimpleDevicePassword", "1");
		DOMUtils.createElement(provDoc, "DevicePasswordExpiration");
		p(provDoc, "DevicePasswordHistory", "0");

		if (protocolVersion > 12.0) {
			p(provDoc, "AllowStorageCard", "1");
			p(provDoc, "AllowCamera", "1");
			p(provDoc, "RequireDeviceEncryption", "0");
			p(provDoc, "AllowUnsignedApplications", "1");
			p(provDoc, "AllowUnsignedInstallationPackages", "1");

			p(provDoc, "MinDevicePasswordComplexCharacters", "3");
			p(provDoc, "AllowWiFi", "1");
			p(provDoc, "AllowTextMessaging", "1");
			p(provDoc, "AllowPOPIMAPEmail", "1");
			p(provDoc, "AllowBluetooth", "2");
			p(provDoc, "AllowIrDA", "1");
			p(provDoc, "RequireManualSyncWhenRoaming", "0");
			p(provDoc, "AllowDesktopSync", "1");
			p(provDoc, "MaxCalendarAgeFilter", "0");
			p(provDoc, "AllowHTMLEmail", "1");
			p(provDoc, "MaxEmailAgeFilter", "0");
			p(provDoc, "MaxEmailBodyTruncationSize", "-1");
			p(provDoc, "MaxEmailHTMLBodyTruncationSize", "-1");

			p(provDoc, "RequireSignedSMIMEMessages", "0");
			p(provDoc, "RequireEncryptedSMIMEMessages", "0");
			p(provDoc, "RequireSignedSMIMEAlgorithm", "0");
			p(provDoc, "RequireEncryptionSMIMEAlgorithm", "0");
			p(provDoc, "AllowSMIMEEncryptionAlgorithmNegotiation", "2");
			p(provDoc, "AllowSMIMESoftCerts", "1");
			p(provDoc, "AllowBrowser", "1");
			p(provDoc, "AllowConsumerEmail", "1");

			p(provDoc, "AllowRemoteDesktop", "1");
			p(provDoc, "AllowInternetSharing", "1");
			DOMUtils.createElement(provDoc, "UnapprovedInROMApplicationList");
			DOMUtils.createElement(provDoc, "ApprovedApplicationList");
		}

		if (protocolVersion > 14) {
			// nothing new here
		}
	}

}
