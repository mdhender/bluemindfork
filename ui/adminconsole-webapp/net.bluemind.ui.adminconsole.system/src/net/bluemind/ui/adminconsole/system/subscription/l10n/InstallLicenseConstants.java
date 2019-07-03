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
package net.bluemind.ui.adminconsole.system.subscription.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.ConstantsWithLookup;

public interface InstallLicenseConstants extends ConstantsWithLookup {

	public static final InstallLicenseConstants INST = GWT.create(InstallLicenseConstants.class);

	String title();

	String uploadLicense();

	String deleteLicense();

	String invalid();

	String failToDeploy();

	String unknown();

	String noLicense();

	String licenseInformation();

	String lic_signed_customer_code();

	String lic_signed_customer();

	String lic_signed_license_available_before();

	String licStarts();

	String licEnds();

	String lic_signed_license_type();

	String lic_signed_part();

	String unableToDetermineInstalledLicense();

	String licenseNoInformations();

	String updateLicenseButton();

	String licFullAccounts();

	String installFullAccounts();

	String subscriptionUpdated();

	String subscriptionDeleted();

	String bmVersion();

	String subscriptionMaxAccounts();

	String installationAccounts();

	String subscriptionMaxSimpleAccounts();

	String installationSimpleAccounts();

	String subscriptionStarts();

	String subscriptionEnds();

	String subscriptionIdentifier();

	String subscriptionPostInstallInformations();

}
