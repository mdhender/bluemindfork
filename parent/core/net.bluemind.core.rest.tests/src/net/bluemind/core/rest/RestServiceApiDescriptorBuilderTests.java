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
package net.bluemind.core.rest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;

import jakarta.ws.rs.HttpMethod;

import org.junit.Test;

import net.bluemind.core.rest.model.RestServiceApiDescriptor;
import net.bluemind.core.rest.model.RestServiceApiDescriptor.MethodDescriptor;
import net.bluemind.core.rest.tests.services.IRestSecuredTestService;
import net.bluemind.core.rest.tests.services.IRestTestService;

public class RestServiceApiDescriptorBuilderTests {
	private RestServiceApiDescriptorBuilder builder = new RestServiceApiDescriptorBuilder();

	@Test
	public void testBuild() {
		RestServiceApiDescriptor bd = builder.build(IRestTestService.class);
		// we test at least 11 methods, leave the rest untested..
		assertTrue(bd.methods.length > 11);

		for (int i = 0; i < bd.methods.length; i++) {
			RestServiceApiDescriptor.MethodDescriptor bind = bd.methods[i];
			if (bind.getApiInterfaceName().equals("sayHelloPath")) {
				assertEquals("/api/test/{before}/{inPath}", bind.path);
				assertEquals(HttpMethod.GET, bind.httpMethodName);
			} else if (bind.getApiInterfaceName().equals("sayHello")) {
				assertEquals("/api/test/{before}/hello", bind.path);
				assertEquals(HttpMethod.GET, bind.httpMethodName);
			} else if (bind.getApiInterfaceName().equals("noResponse")) {
				assertEquals("/api/test/voidResponse", bind.path);
				assertEquals(HttpMethod.POST, bind.httpMethodName);
			} else if (bind.getApiInterfaceName().equals("noRequest")) {
				assertEquals("/api/test/voidRequest", bind.path);
				assertEquals(HttpMethod.POST, bind.httpMethodName);
			} else if (bind.getApiInterfaceName().equals("generic1")) {
				assertEquals("/api/test/generic/1", bind.path);
				assertEquals(HttpMethod.POST, bind.httpMethodName);
			} else if (bind.getApiInterfaceName().equals("put")) {
				assertEquals("/api/test/{uid}", bind.path);
				assertEquals(HttpMethod.PUT, bind.httpMethodName);
			} else if (bind.getApiInterfaceName().equals("complex")) {
				assertEquals("/api/test/complexe", bind.path);
				assertEquals(HttpMethod.POST, bind.httpMethodName);
			} else if (bind.getApiInterfaceName().equals("param")) {
				assertEquals("/api/test/queryParam", bind.path);
				assertEquals(HttpMethod.GET, bind.httpMethodName);
			} else if (bind.getApiInterfaceName().equals("putTime")) {
				assertEquals("/api/test/dateTime", bind.path);
				assertEquals(HttpMethod.PUT, bind.httpMethodName);
			} else {
				System.err.println("not handled method " + bind.getApiInterfaceName() + " " + bind.path);
			}
		}

	}

	@Test
	public void testRequiredAnnotation() {
		RestServiceApiDescriptor bd = builder.build(IRestSecuredTestService.class);
		assertEquals(3, bd.methods.length);

		for (MethodDescriptor m : bd.methods) {

			if (m.getApiInterfaceName().equals("helloSimple")) {
				assertEquals(1, m.roles.length);
				assertEquals("role1", m.roles[0]);
			} else if (m.getApiInterfaceName().equals("helloSlave")) {
				assertEquals(1, m.roles.length);

				assertEquals(new HashSet<>(Arrays.asList("slave")), new HashSet<>(Arrays.asList(m.roles)));
			} else if (m.getApiInterfaceName().equals("helloMaster")) {
				assertEquals(1, m.roles.length);

				assertEquals(new HashSet<>(Arrays.asList("master")), new HashSet<>(Arrays.asList(m.roles)));
			} else {
				fail("unknown method " + m.getApiInterfaceName());
			}

		}
	}

	@Test
	public void testParsingInterfaceWithRolesOnClass() {
		RestServiceApiDescriptor bd = builder.build(IRestServiceRolesOnClass.class);

		RestServiceApiDescriptor.MethodDescriptor methodBind = bd.methods[0];

		assertArrayEquals(sort(methodBind.roles), sort(new String[] { "canExecute1", "canRead2" }));
	}

	@Test
	public void testParsingInterfaceWithRolesOnMethod() {
		RestServiceApiDescriptor bd = builder.build(IRestServiceRolesOnMethod.class);

		RestServiceApiDescriptor.MethodDescriptor methodBind = bd.methods[0];

		assertArrayEquals(sort(methodBind.roles), sort(new String[] { "canDo1", "canDo2" }));
	}

	@Test
	public void testParsingInterfaceWithRolesOnClassAndMethodShouldUseMethodAnnotations() {
		RestServiceApiDescriptor bd = builder.build(IRestServiceRolesOnClassAndMethod.class);

		RestServiceApiDescriptor.MethodDescriptor methodBind = getMethodBind(bd, "foo");

		assertArrayEquals(sort(methodBind.roles), sort(new String[] { "canDo1", "canDo2" }));
	}

	@Test
	public void testParsingInterfaceWithRolesOnClassAndMethodShouldUseClassAnnotationsOnMethodsWithoutAnnotations() {
		RestServiceApiDescriptor bd = builder.build(IRestServiceRolesOnClassAndMethod.class);

		RestServiceApiDescriptor.MethodDescriptor methodBind = getMethodBind(bd, "bar");

		assertArrayEquals(sort(methodBind.roles), sort(new String[] { "canExecute1", "canRead2" }));
	}

	private String[] sort(String[] roles) {
		Arrays.sort(roles);
		return roles;
	}

	private MethodDescriptor getMethodBind(RestServiceApiDescriptor bd, String methodName) {
		for (int i = 0; i < bd.methods.length; i++) {
			RestServiceApiDescriptor.MethodDescriptor methodBind = bd.methods[i];
			if (methodBind.interfaceMethod.getName().contains(methodName)) {
				return methodBind;
			}
		}
		return null;
	}

	@Test
	public void testApiValidity() {
		try {
			builder.build(IBadService.class);
			fail();
		} catch (RuntimeException e) {
		}
	}
}
