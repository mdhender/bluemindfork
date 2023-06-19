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

import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SmimeCacertInfos {

	public String cacertUid;
	public String cacertIssuer;
	public String cacertSubject;
	public List<SmimeRevocation> revocations;

	public static SmimeCacertInfos create(String cacertIssuer, String cacertSubject,
			List<SmimeRevocation> revocations) {
		SmimeCacertInfos infos = new SmimeCacertInfos();
		infos.cacertIssuer = cacertIssuer;
		infos.cacertSubject = cacertSubject;
		infos.revocations = revocations;
		return infos;
	}

	public static SmimeCacertInfos create(String cacertUid, String cacertIssuer, String cacertSubject,
			List<SmimeRevocation> revocations) {
		SmimeCacertInfos infos = create(cacertIssuer, cacertSubject, revocations);
		infos.cacertUid = cacertUid;
		return infos;
	}

}
