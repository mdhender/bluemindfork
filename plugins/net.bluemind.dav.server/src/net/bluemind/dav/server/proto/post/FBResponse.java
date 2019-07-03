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
package net.bluemind.dav.server.proto.post;

import java.util.List;
import java.util.Map;

import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.dav.server.ics.FreeBusy.CalRequest;

public class FBResponse extends PostResponse {

	private Map<String, VFreebusy> fbRanges;
	private List<CalRequest> req;

	public FBResponse(List<CalRequest> requests, Map<String, VFreebusy> infos) {
		this.req = requests;
		this.fbRanges = infos;
	}

	public Map<String, VFreebusy> getFbRanges() {
		return fbRanges;
	}

	public List<CalRequest> getReq() {
		return req;
	}

}
