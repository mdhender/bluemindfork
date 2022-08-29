package net.bluemind.group.api;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;

@BMApi(version = "3")
public interface IGroupMember {

	/**
	 * Retrieves a list of all {@link net.bluemind.group.api.Group}s this uid is
	 * member of.
	 * 
	 * @param uid the member's unique id
	 * @return a list of groups
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{uid}/groups")
	List<ItemValue<Group>> memberOf(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Retrieves a list of all {@link net.bluemind.group.api.Group} uids this uid is
	 * member of.
	 * 
	 * @param uid the member's unique id
	 * @return a list of {@link net.bluemind.group.api.Group} uids
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{uid}/groupUids")
	List<String> memberOfGroups(@PathParam(value = "uid") String uid) throws ServerFault;

}
