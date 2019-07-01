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
package net.bluemind.core.rest.vertx;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Matchers;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.base.RestServiceSecurityCheck;

public class RestServiceSecurityCheckTest {

	private RestServiceSecurityCheck getService(String... requiredRoles) {
		RestServiceSecurityCheck service = spy(new RestServiceSecurityCheck(Arrays.asList(requiredRoles), null));

		doNothing().when(service).invokeNext(Matchers.<SecurityContext> any(), Matchers.<Object> any(),
				Matchers.<Object[]> any(), Matchers.<AsyncHandler<Object>> any());

		doNothing().when(service).executeFaultHandler(Matchers.<AsyncHandler<Object>> any());

		return service;
	}

	private SecurityContext getWithRoles(String... userRoles) {
		SecurityContext context = new SecurityContext(null, null, Collections.<String> emptyList(),
				Arrays.asList(userRoles), null);
		return context;
	}

	@Test
	public void testExecutingServiceRequiringNoRolesShouldAlwaysPass() {
		RestServiceSecurityCheck service = getService();

		service.invoke(getWithRoles(), null, null, null);
		service.invoke(getWithRoles("canDoAnything1", "canDoAnything2"), null, null, null);

		verify(service, times(2)).invokeNext(Matchers.<SecurityContext> any(), Matchers.<Object> any(),
				Matchers.<Object[]> any(), Matchers.<AsyncHandler<Object>> any());

		verify(service, times(0)).executeFaultHandler(Matchers.<AsyncHandler<Object>> any());
	}

	@Test
	public void testExecutingServiceRequiringOneRoleShouldFailIfNoRoleIsProvided() {
		RestServiceSecurityCheck service = getService("canAccessMethod");

		service.invoke(getWithRoles(), null, null, null);

		verify(service, times(0)).invokeNext(Matchers.<SecurityContext> any(), Matchers.<Object> any(),
				Matchers.<Object[]> any(), Matchers.<AsyncHandler<Object>> any());

		verify(service, times(1)).executeFaultHandler(Matchers.<AsyncHandler<Object>> any());
	}

	@Test
	public void testExecutingServiceRequiringOneRoleShouldFailIfWrongRoleIsProvided() {
		RestServiceSecurityCheck service = getService("canAccessMethod");

		service.invoke(getWithRoles("canDoSomethingElse"), null, null, null);

		verify(service, times(0)).invokeNext(Matchers.<SecurityContext> any(), Matchers.<Object> any(),
				Matchers.<Object[]> any(), Matchers.<AsyncHandler<Object>> any());

		verify(service, times(1)).executeFaultHandler(Matchers.<AsyncHandler<Object>> any());
	}

	@Test
	public void testExecutingServiceWithMultipleRolesShouldFailIfNoRoleIsProvided() {
		RestServiceSecurityCheck service = getService("canAccessMethod", "isAdmin");

		service.invoke(getWithRoles(), null, null, null);

		verify(service, times(0)).invokeNext(Matchers.<SecurityContext> any(), Matchers.<Object> any(),
				Matchers.<Object[]> any(), Matchers.<AsyncHandler<Object>> any());

		verify(service, times(1)).executeFaultHandler(Matchers.<AsyncHandler<Object>> any());
	}

	@Test
	public void testExecutingServiceWithMultipleRoleShouldFailIfWrongRoleIsProvided() {
		RestServiceSecurityCheck service = getService("canAccessMethod", "isAdmin");

		service.invoke(getWithRoles("canDoSomethingElse"), null, null, null);

		verify(service, times(0)).invokeNext(Matchers.<SecurityContext> any(), Matchers.<Object> any(),
				Matchers.<Object[]> any(), Matchers.<AsyncHandler<Object>> any());

		verify(service, times(1)).executeFaultHandler(Matchers.<AsyncHandler<Object>> any());
	}

	@Test
	public void testExecutingServiceWithMultipleRoleShouldSucceedIfMultipleCorrectRoleAreProvided() {
		RestServiceSecurityCheck service = getService("canAccessMethod", "isAdmin", "isRoot");

		service.invoke(getWithRoles("canAccessMethod", "isAdmin"), null, null, null);

		verify(service, times(1)).invokeNext(Matchers.<SecurityContext> any(), Matchers.<Object> any(),
				Matchers.<Object[]> any(), Matchers.<AsyncHandler<Object>> any());

		verify(service, times(0)).executeFaultHandler(Matchers.<AsyncHandler<Object>> any());
	}

	@Test
	public void testExecutingServiceWithMultipleRoleShouldSucceedIfAllCorrectRoleAreProvided() {
		RestServiceSecurityCheck service = getService("canAccessMethod", "isAdmin", "isRoot");

		service.invoke(getWithRoles("canAccessMethod", "isAdmin", "isRoot"), null, null, null);

		verify(service, times(1)).invokeNext(Matchers.<SecurityContext> any(), Matchers.<Object> any(),
				Matchers.<Object[]> any(), Matchers.<AsyncHandler<Object>> any());

		verify(service, times(0)).executeFaultHandler(Matchers.<AsyncHandler<Object>> any());
	}

	@Test
	public void testExecutingServiceWithMultipleRoleShouldSucceedIfOneCorrectRoleIsProvided() {
		RestServiceSecurityCheck service = getService("canAccessMethod", "isAdmin", "isRoot");

		service.invoke(getWithRoles("isAdmin"), null, null, null);

		verify(service, times(1)).invokeNext(Matchers.<SecurityContext> any(), Matchers.<Object> any(),
				Matchers.<Object[]> any(), Matchers.<AsyncHandler<Object>> any());

		verify(service, times(0)).executeFaultHandler(Matchers.<AsyncHandler<Object>> any());
	}

}
