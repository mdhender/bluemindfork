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
package net.bluemind.webmodule.server;

import java.util.Collection;

import net.bluemind.webmodule.server.forward.ForwardedLocation;

public class WebserverConfiguration {

	public static final String BM_SSO_XML = "/etc/bm-webserver/bm_sso.xml";

	private Collection<ForwardedLocation> forwardedLocations;

	public Collection<ForwardedLocation> getForwardedLocations() {
		return forwardedLocations;
	}

	public void setForwardedLocations(Collection<ForwardedLocation> forwardedLocations) {
		this.forwardedLocations = forwardedLocations;
	}

}