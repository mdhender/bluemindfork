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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@Path("/test")
@BMApi(version = "3")
public interface IRestTestService extends IVirtualApi<ComplexRequest> {

	@GET
	@Path("queryparams")
	public String sayWithQueryParams(@QueryParam("lastring") String lastring, @QueryParam("leint") int value,
			@QueryParam("lebool") boolean bool);

	@GET
	@Path("{before}/hello")
	public String sayHello(@PathParam("before") String helloPhrase) throws ServerFault;

	@POST
	@Path("hello2")
	public String sayHello2(String helloPhrase) throws ServerFault;

	@POST
	@Path("complexe")
	public ComplexResponse complex(ComplexRequest request) throws ServerFault;

	@GET
	@Path("{before}/{inPath}")
	public String sayHelloPath(@PathParam("before") String helloPhrase, @PathParam("inPath") String after)
			throws ServerFault;

	@POST
	@Path("voidResponse")
	public void noResponse(ComplexRequest request);

	/**
	 * This method blocks
	 */
	@POST
	@Path("gloryHole")
	public void blackHole();

	@POST
	@Path("nullResponse")
	public ComplexResponse nullResponse(ComplexRequest request) throws ServerFault;

	@POST
	@Path("voidRequest")
	public ComplexResponse noRequest() throws ServerFault;

	@POST
	@Path("generic/1")
	public GenericResponse<ComplexResponse> generic1(GenericResponse<ComplexRequest> req) throws ServerFault;

	@PUT
	@Path("{uid}")
	public void put(@PathParam("uid") String uid, ComplexRequest request) throws ServerFault;

	@GET
	@Path("queryParam")
	public ComplexResponse param(@QueryParam("param1") String test, @QueryParam("param2") Long test2,
			@QueryParam("param3") ParamEnum test3) throws ServerFault;

	@PUT
	@Path("dateTime")
	public ObjectWithTime putTime(ObjectWithTime time) throws ServerFault;

	@POST
	@Path("throwSpecificServerFault")
	public void throwSpecificServerFault() throws ServerFault;

	@POST
	@Path("throwNullpointer")
	public void throwNullpointer() throws ServerFault;

	@GET
	@Path("mime")
	@Produces("application/binary")
	public String mime() throws ServerFault;

	@GET
	@Path("bytearray")
	@Produces("application/binary")
	public byte[] bytearray() throws ServerFault;

	@GET
	@Path("longinparam/{uid}")
	public String longInPathParam(@PathParam("uid") long uid) throws ServerFault;

}
