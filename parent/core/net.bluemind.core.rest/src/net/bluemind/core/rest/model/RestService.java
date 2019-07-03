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
package net.bluemind.core.rest.model;

import java.lang.reflect.Method;

import net.bluemind.core.rest.model.RestServiceApiDescriptor.MethodDescriptor;

public class RestService {

	public final RestServiceApiDescriptor descriptor;
	public final Endpoint endpoint;

	public RestService(RestServiceApiDescriptor descriptor, Endpoint endpoint) {
		this.descriptor = descriptor;
		this.endpoint = endpoint;
	}

	public static String address(RestService restService, MethodDescriptor methodDesciptor) {
		return "core.services-" + restService.descriptor.getApiInterfaceName() + "."
				+ methodDesciptor.getApiInterfaceName();
	}

	public static String address(Class<?> endpointInterface, Method endpointMethod) {
		return "core.services-" + endpointInterface.getName() + "." + endpointMethod.getName();
	}
}
