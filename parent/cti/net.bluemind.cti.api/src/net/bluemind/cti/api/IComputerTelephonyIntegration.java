package net.bluemind.cti.api;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/cti/{domainUid}/{userUid}")
public interface IComputerTelephonyIntegration {

	@POST
	@Path("dial")
	void dial(@QueryParam("number") String number) throws ServerFault;

	/**
	 * Retrieve user phone status
	 * 
	 * @return phone status
	 * @throws ServerFault
	 */
	@GET
	@Path("status")
	Status getStatus() throws ServerFault;

	/**
	 * Set phone status for a component
	 * 
	 * @param component
	 * @param status
	 * @throws ServerFault
	 */
	@POST
	@Path("status/{component}")
	void setStatus(@PathParam("component") String component, Status status) throws ServerFault;

	/**
	 * Set forward for a componnent
	 * 
	 * @param component
	 * @param phoneNumber
	 * @throws ServerFault
	 */
	@POST
	@Path("forward/{component}")
	void forward(@PathParam("component") String component, @QueryParam("phoneNumber") String phoneNumber)
			throws ServerFault;

}
