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
package net.bluemind.core.rest.tests.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;

public class RestTestServiceTests {

	public IRestTestService getRestTestService(SecurityContext context) {
		return new RestServiceImpl(context);
	}

	public IRestPathTestService getRestPathTestService(SecurityContext context, String param1, String param2) {
		return new RestPathTestServiceImpl(param1, param2);
	}

	@Test
	public void testReturnNull() throws Exception {
		assertEquals(null, getRestTestService(SecurityContext.ANONYMOUS).nullResponse(new ComplexRequest()));
	}

	@Test
	public void testSayHello() throws Exception {
		String ret = getRestTestService(SecurityContext.ANONYMOUS).sayHello("toto€");
		Assert.assertEquals("hello toto€ anonymous", ret);
		ret = getRestTestService(SecurityContext.ANONYMOUS).sayHello("toto/path/working");
		Assert.assertEquals("hello toto/path/working anonymous", ret);
	}

	@Test
	public void testSayHello2() throws Exception {
		String ret = getRestTestService(SecurityContext.ANONYMOUS).sayHello2("toto€");
		Assert.assertEquals("hello2 toto€", ret);
	}

	@Test
	public void testThrowNullPoint() throws Exception {
		for (int i = 0; i < 10; i++) {
			try {
				getRestTestService(SecurityContext.ANONYMOUS).throwNullpointer();
				fail();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testPerf() throws Exception {
		IRestTestService service = getRestTestService(SecurityContext.ANONYMOUS);
		for (int i = 0; i < 10000; i++) {
			try {
				service.sayHello2("toto€");
			} catch (Exception e) {
				e.getCause().printStackTrace();
				throw e;
			}
		}

		long time = System.nanoTime();
		for (int i = 0; i < 10000; i++) {
			service.sayHello2("toto€");
		}

		long elapsed = System.nanoTime() - time;

		System.out.println("Time to make 10000 call " + ((double) elapsed / 1000000.0));
		System.out.println("Time by call " + ((double) elapsed / (1000000.0 * 10000.0)));
	}

	@Test
	public void testComplexe() throws Exception {

		ComplexRequest req = new ComplexRequest();
		req.setValue1("v€");
		req.setValue2("v2");

		ComplexResponse r = getRestTestService(SecurityContext.ANONYMOUS).complex(req);
		assertEquals("v€v2", r.getValue1Plus2());
		assertEquals("v2v€", r.getValue2Plus1());
		assertEquals("anonymous", r.getSubject());
	}

	@Test
	public void testHelloPath() throws Exception {
		IRestTestService s = getRestTestService(SecurityContext.ANONYMOUS);

		for (int i = 0; i < 10; i++) {
			String resp = s.sayHelloPath("jojo", "!");
			assertEquals("hello jojo !", resp);
		}
	}

	@Test
	public void testNoResponse() throws ServerFault {
		ComplexRequest req = new ComplexRequest();
		req.setValue1("v1");
		req.setValue2("v2");
		getRestTestService(SecurityContext.ANONYMOUS).noResponse(req);
	}

	@Test
	public void testNoRequest() throws Exception {
		ComplexResponse resp = getRestTestService(SecurityContext.ANONYMOUS).noRequest();
		assertEquals("OK", resp.getSubject());
	}

	@Test
	public void testGeneric1() throws Exception {

		GenericResponse<ComplexRequest> req = new GenericResponse<>();
		req.message = "coucou";
		req.value = new ComplexRequest();
		req.value.setValue1("OK");
		GenericResponse<ComplexResponse> r = getRestTestService(SecurityContext.ANONYMOUS).generic1(req);

		assertEquals("OK", r.message);
		System.out.println("value " + r.value);
		assertTrue(r.value instanceof ComplexResponse);
		assertEquals("OK", r.value.getSubject());

	}

	@Test
	public void testPut() throws ServerFault {
		ComplexRequest req = new ComplexRequest();
		req.setValue1("v1");
		req.setValue2("v2");

		getRestTestService(SecurityContext.ANONYMOUS).put("test", req);

	}

	@Test
	public void testParam() throws Exception {
		ComplexResponse resp = getRestTestService(SecurityContext.ANONYMOUS).param("test1", Long.valueOf(666),
				ParamEnum.Test2);
		assertEquals("test1", resp.getSubject());
		assertEquals("666", resp.getValue1Plus2());
		assertEquals(ParamEnum.Test2, resp.getParamEnum());
	}

	@Test
	public void testGoodMorning() {
		String resp = getRestPathTestService(SecurityContext.ANONYMOUS, "1", "2").goodMorning("jojo");
		assertEquals("[1][2]good morning jojo", resp);
	}

	@Test
	public void testRootParamEncodingGoodMorning() {
		String resp = getRestPathTestService(SecurityContext.ANONYMOUS, "root/sub:ok", "2").goodMorning("jojo");
		assertEquals("[root/sub:ok][2]good morning jojo", resp);
	}

	@Test
	public void testPutTime() throws Exception {
		ObjectWithTime t = new ObjectWithTime();
		t.subject = "test";
		t.date1 = BmDateTimeWrapper.fromTimestamp(123l, "Asia/Ho_Chi_Minh", Precision.DateTime);
		ObjectWithTime resp = getRestTestService(SecurityContext.ANONYMOUS).putTime(t);
		assertEquals("result", resp.subject);
		assertEquals(t.date1, resp.date1);
		assertEquals("Asia/Ho_Chi_Minh", resp.date1.timezone);
		assertNull(t.date2);
		assertEquals("postResult", resp.post);
	}

	@Test
	public void testSayWithQueryParams() {
		assertEquals("+lach&ain€+" + 40 + true,
				getRestTestService(SecurityContext.ANONYMOUS).sayWithQueryParams("+lach&ain€+", 40, true));
	}

	@Test
	public void testThrowSpecificServerFault() {
		for (int i = 0; i < 10; i++) {
			try {

				getRestTestService(SecurityContext.ANONYMOUS).throwSpecificServerFault();
				fail("should not go here");
			} catch (ServerFault e) {
				assertEquals(ErrorCode.ALREADY_EXISTS, e.getCode());
			}
		}
	}

	@Test
	public void testBytearray() throws Exception {
		byte[] resp = getRestTestService(SecurityContext.ANONYMOUS).bytearray();
		assertEquals("hello", new String(resp));
	}

	@Test
	public void testLongInPathParam() throws Exception {
		String ret = getRestTestService(SecurityContext.ANONYMOUS).longInPathParam(42);
		Assert.assertEquals("hello 42", ret);
	}

	@Test
	public void testVirtual() throws Exception {
		String ret = getRestTestService(SecurityContext.ANONYMOUS).checkType(new ComplexRequest());
		Assert.assertEquals("ok", ret);
	}

}
