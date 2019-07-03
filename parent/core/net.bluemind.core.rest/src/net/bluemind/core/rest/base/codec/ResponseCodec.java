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
package net.bluemind.core.rest.base.codec;

import java.lang.reflect.Type;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;

public interface ResponseCodec<T> {

	public interface Factory<T> {
		ResponseCodec<?> create(Type type, String defaultMimeType);
	}

	RestResponse encode(RestRequest request, String defaultMimeType, T response);

	RestResponse encodeFault(RestRequest request, String defaultMimeType, Throwable fault);

	T decode(RestResponse response) throws ServerFault;

}
