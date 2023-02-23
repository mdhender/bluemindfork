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
package net.bluemind.smime.cacerts.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Required;

@BMApi(version = "3")
public class RevocationResult {

	@BMApi(version = "3")
	public enum RevocationStatus {
		REVOKED, NOT_REVOKED
	}

	@Required
	public SmimeRevocation revocation;
	@Required
	public RevocationStatus status = RevocationStatus.NOT_REVOKED;

	/**
	 * Create a revoked RevocationResult
	 * 
	 * @param revocation the certificate client revoked
	 * @return RevocationResult
	 */
	public static RevocationResult revoked(SmimeRevocation revocation) {
		RevocationResult revokedResult = new RevocationResult();
		revokedResult.revocation = revocation;
		revokedResult.status = RevocationStatus.REVOKED;
		return revokedResult;
	}

	/**
	 * Create a NOT revoked RevocationResult
	 * 
	 * @param revocation the certificate client not revoked
	 * 
	 * @return RevocationResult
	 */
	public static RevocationResult notRevoked(SmimeRevocation revocation) {
		RevocationResult revokedResult = new RevocationResult();
		revokedResult.revocation = revocation;
		revokedResult.status = RevocationStatus.NOT_REVOKED;
		return revokedResult;
	}

}
