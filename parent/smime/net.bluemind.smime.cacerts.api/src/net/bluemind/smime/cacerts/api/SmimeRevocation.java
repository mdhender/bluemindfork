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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.smime.cacerts.api;

import java.util.Date;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SmimeRevocation extends SmimeCertClient {

	public Date revocationDate;
	public String revocationReason;
	public String url;
	public Date lastUpdate;
	public Date nextUpdate;
	public String cacertItemUid;

	public static SmimeRevocation create(String serialNumber, Date revocationDate, String revocationReason, String url,
			Date lastUpdate, Date nextUpdate, String issuer, String cacertItemUid) {
		SmimeRevocation revocation = SmimeRevocation.create(serialNumber, issuer);

		revocation.revocationDate = revocationDate;
		revocation.revocationReason = revocationReason;
		revocation.url = url;
		revocation.lastUpdate = lastUpdate;
		revocation.nextUpdate = nextUpdate;
		revocation.cacertItemUid = cacertItemUid;

		return revocation;
	}

	public static SmimeRevocation create(String serialNumber, String issuer) {
		SmimeRevocation revocation = new SmimeRevocation();
		revocation.serialNumber = serialNumber;
		revocation.issuer = issuer;
		return revocation;
	}

	@Override
	public String toString() {
		return "SmimeRevocation [serialNumber=" + serialNumber + ", revocationDate=" + revocationDate
				+ ", revocationReason=" + revocationReason + ", url=" + url + ", lastUpdate=" + lastUpdate
				+ ", nextUpdate=" + nextUpdate + ", issuer=" + issuer + ", cacertItemUid=" + cacertItemUid + "]";

	}

}
