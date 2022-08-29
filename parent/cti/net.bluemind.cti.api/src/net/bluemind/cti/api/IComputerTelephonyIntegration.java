package net.bluemind.cti.api;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

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
	 * Retrieve users email list from the cti implementation
	 * 
	 * @return users email list
	 * @throws ServerFault
	 */
	@GET
	@Path("users")
	List<String> getUserEmails() throws ServerFault;

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
