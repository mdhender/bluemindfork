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
package net.bluemind.core.rest.http.vertx;

import net.bluemind.core.api.fault.ServerFault;

public class HttpStatusCode {
	public final int code;

	private HttpStatusCode(int code) {
		this.code = code;
	}

	public static HttpStatusCode fromException(ServerFault e) {
		int statusCode = 500;
		switch (e.getCode()) {
		case INVALID_PARAMETER:
		case INVALID_EMAIL:
			statusCode = 400;
			break;
		case NOT_FOUND:
			statusCode = 404;
			break;
		case PERMISSION_DENIED:
		case FORBIDDEN:
			statusCode = 403;
			break;
		case ENTITY_TOO_LARGE:
			statusCode = 413;
			break;
		default:
			statusCode = 500;
		}
		return new HttpStatusCode(statusCode);
	}

}
