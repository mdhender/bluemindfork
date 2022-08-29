/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.videoconferencing.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@Path("/videoconferencing/uids")
public interface IVideoConferenceUids {

	public static final String RESOURCETYPE_UID = "bm-videoconferencing";
	public static final String PROVIDER_TYPE = "bm-videoconferencing-type";

	@GET
	@Path("resource_type_uid")
	public default String getResourceTypeUid() {
		return IVideoConferenceUids.resourceTypeUid();
	}

	@GET
	@Path("provider_type")
	public default String getProviderTypeUid() {
		return IVideoConferenceUids.providerTypeUid();
	}

	public static String resourceTypeUid() {
		return IVideoConferenceUids.RESOURCETYPE_UID;
	}

	public static String providerTypeUid() {
		return IVideoConferenceUids.PROVIDER_TYPE;
	}

}
