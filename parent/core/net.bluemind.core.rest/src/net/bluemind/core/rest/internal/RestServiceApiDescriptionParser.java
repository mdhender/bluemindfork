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
package net.bluemind.core.rest.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import net.bluemind.common.reflect.ClassVisitor;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.RequiredRoles;
import net.bluemind.core.rest.model.RestServiceApiDescriptor;
import net.bluemind.core.rest.model.RestServiceApiDescriptor.MethodDescriptor;

public class RestServiceApiDescriptionParser implements ClassVisitor {
	private Class<?> clazz;
	private List<MethodDescriptor> methods = new ArrayList<>();
	private List<String> requiredRoles = new ArrayList<>();
	private String rootPath;

	@Override
	public void visit(Class<?> clazz) {
		this.clazz = clazz;
		if (!classIsBmApiAnnotated(clazz)) {
			throw new RuntimeException("class " + clazz.getName() + " doesnt have BMApi annotation");
		}

		parseRootPath(clazz);
		parseRoles(clazz);
	}

	@Override
	public void visit(Method method) {
		MethodDescriptor methodDescriptor = createMethodDescriptor(clazz, method);
		if (null != methodDescriptor) {
			methods.add(methodDescriptor);
		}
	}

	public RestServiceApiDescriptor getDescriptor() {
		return new RestServiceApiDescriptor(clazz, methods.toArray(new MethodDescriptor[0]), rootPath);
	}

	private void parseRootPath(Class<?> clazz) {
		Path rootPathAnnotation = clazz.getAnnotation(Path.class);
		BMApi apiAnnotation = clazz.getAnnotation(BMApi.class);
		if (rootPathAnnotation != null) {
			if (apiAnnotation.internal()) {
				rootPath = "/internal-api" + rootPathAnnotation.value();
			} else {
				rootPath = "/api" + rootPathAnnotation.value();
			}
		}
	}

	private void parseRoles(Class<?> clazz) {
		RequiredRoles roles = clazz.getAnnotation(RequiredRoles.class);
		if (null != roles) {
			requiredRoles.addAll(Arrays.asList(roles.value()));
		}
	}

	private MethodDescriptor createMethodDescriptor(Class<?> clazz, Method method) {
		HttpMethod httpMethod = getMethod(method);
		if (httpMethod == null) {
			return null;
		}
		String path = buildPath(clazz, method);
		String[] roles = parseMethodRoles(method);
		String[] produces = parseMethodProduces(method);
		MethodDescriptor methodDescriptor = new MethodDescriptor(httpMethod.value(), path, method, roles, produces);

		return methodDescriptor;
	}

	private String[] parseMethodRoles(Method method) {
		Set<String> methodRoles = new HashSet<>();

		RequiredRoles required = method.getAnnotation(RequiredRoles.class);
		// role annotations on methods have higher priority than class
		// annotations
		if (required != null) {
			methodRoles.addAll(Arrays.asList(required.value()));
		} else {
			methodRoles.addAll(requiredRoles);
		}

		return methodRoles.toArray(new String[0]);
	}

	private String[] parseMethodProduces(Method method) {

		Produces p = method.getAnnotation(Produces.class);
		if (p != null) {
			return p.value();
		} else {
			return new String[] { "application/json" };
		}
	}

	private String buildPath(Class<?> clazz, Method method) {
		Path rootPath = clazz.getAnnotation(Path.class);
		BMApi apiAnnotation = clazz.getAnnotation(BMApi.class);
		Path methodPath = method.getAnnotation(Path.class);

		String path = rootPath != null ? rootPath.value() : "";

		if (methodPath != null) {
			path = String.format("%s/%s", path, methodPath.value());
		}
		return (apiAnnotation.internal() ? "/internal-api" : "/api") + path;
	}

	private HttpMethod getMethod(Method method) {
		for (Annotation annotation : method.getAnnotations()) {
			if (annotation.getClass().isInstance(HttpMethod.class)) {
				return HttpMethod.class.cast(annotation);
			} else if (annotation.annotationType().getAnnotation(HttpMethod.class) != null) {
				return annotation.annotationType().getAnnotation(HttpMethod.class);
			}
		}
		return null;
	}

	private boolean classIsBmApiAnnotated(Class<?> clazz) {
		return null != clazz.getAnnotation(BMApi.class);
	}

	public void validate() {
		// check some rules
		// 1. two methods of a same class cannot have the same methodName
		Set<String> names = new HashSet<>();
		for (MethodDescriptor m : methods) {
			if (!names.add(m.interfaceMethod.getName())) {
				throw new RuntimeException(
						"two methods of " + clazz.getName() + " have the same name :" + m.interfaceMethod.getName());
			}
		}
	}

}
