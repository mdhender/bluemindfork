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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;

public class RestServiceImpl implements IRestTestService {

	private SecurityContext securityContext;

	public RestServiceImpl(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	@Override
	public String sayHello(String helloPhrase) {
		return "hello " + helloPhrase + " " + securityContext.getSubject();
	}

	@Override
	public String sayHello2(String helloPhrase) {
		return "hello2 " + helloPhrase;
	}

	@Override
	public ComplexResponse complex(ComplexRequest request) {

		ComplexResponse resp = new ComplexResponse();
		resp.setValue1Plus2(request.getValue1() + request.getValue2());
		resp.setValue2Plus1(request.getValue2() + request.getValue1());
		resp.setSubject(securityContext.getSubject());
		return resp;
	}

	@Override
	public String sayHelloPath(String helloPhrase, String after) {
		return "hello " + helloPhrase + " " + after;
	}

	@Override
	public void noResponse(ComplexRequest request) throws ServerFault {
		if (request == null) {
			throw new ServerFault("request is null");
		}

		System.out.println(request.getValue1());
	}

	@Override
	public ComplexResponse noRequest() {
		ComplexResponse resp = new ComplexResponse();
		resp.setSubject("OK");
		return resp;
	}

	@Override
	public GenericResponse<ComplexResponse> generic1(GenericResponse<ComplexRequest> req) {

		GenericResponse<ComplexResponse> resp = new GenericResponse<>();
		resp.message = "OK";
		resp.value = new ComplexResponse();
		resp.value.setSubject(req.value.getValue1());

		return resp;
	}

	@Override
	public void put(String uid, ComplexRequest request) throws ServerFault {
		if (uid == null) {
			throw new ServerFault("uid is null");
		}

		if (request == null) {
			throw new ServerFault("request is null");
		}

		System.out.println("uid " + uid);
		System.out.println("request v1 " + request.getValue1());

		System.out.println("request v2 " + request.getValue2());

	}

	@Override
	public ComplexResponse param(String test, Long test2, ParamEnum test3) {
		ComplexResponse resp = new ComplexResponse();
		resp.setSubject(test);

		resp.setValue1Plus2(test2.toString());
		resp.setParamEnum(test3);
		return resp;
	}

	@Override
	public ObjectWithTime putTime(ObjectWithTime time) {
		time.subject = "result";
		time.post = "postResult";
		return time;
	}

	@Override
	public void throwSpecificServerFault() throws ServerFault {
		throw ServerFault.alreadyExists("obiwan");
	}

	@Override
	public String sayWithQueryParams(String lastring, int value, boolean bool) {
		return lastring + value + bool;
	}

	@Override
	public String mime() {
		return "hello";
	}

	@Override
	public byte[] bytearray() {
		return "hello".getBytes();
	}

	@Override
	public String longInPathParam(long uid) throws ServerFault {
		return "hello " + uid;
	}

	@Override
	public String checkType(ComplexRequest value) {
		if (value instanceof ComplexRequest) {
			return "ok";
		} else {
			return "not ok";
		}
	}

	public void throwNullpointer() throws ServerFault {
		throw new NullPointerException();
	}

	@Override
	public ComplexResponse nullResponse(ComplexRequest request) throws ServerFault {
		return null;
	}

	@Override
	public void blackHole() {
		System.err.println("In black hole... will block forever...");
		try {
			new CompletableFuture<Void>().get(1, TimeUnit.MINUTES);
		} catch (Exception e) {
			// ok
		}
	}

}
