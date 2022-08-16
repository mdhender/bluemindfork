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
import java.lang.reflect.Type;

public final class RestServiceApiDescriptor {

	public final Class<?> apiInterface;
	public final MethodDescriptor[] methods;
	public final String rootPath;

	public RestServiceApiDescriptor(Class<?> apiInterface, MethodDescriptor[] methods, String rootPath) {
		this.apiInterface = apiInterface;
		this.methods = methods;
		this.rootPath = rootPath;
	}

	public static final class MethodDescriptor {
		public final String path;
		public final String httpMethodName;
		public final Method interfaceMethod;
		public final String[] roles;
		public final String[] produces;
		public final Type genericType;

		public MethodDescriptor(String httpMethodName, String path, Method method, String[] roles, String[] produces,
				Type genericType) {
			this.httpMethodName = httpMethodName;
			this.path = path;
			this.interfaceMethod = method;
			this.roles = roles;
			this.produces = produces;
			this.genericType = genericType;
		}

		public String getApiInterfaceName() {
			return interfaceMethod.getName();
		}
	}

	public String getApiInterfaceName() {
		return apiInterface.getCanonicalName();
	}

	@Override
	public String toString() {
		return String.format("%s@%s", getApiInterfaceName(), rootPath);
	}

}
