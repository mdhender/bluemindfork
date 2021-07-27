package net.bluemind.mailshare.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.IDirEntryPhotoSupport;

@BMApi(version = "3")
@Path("/mailshares/{domainUid}")
public interface IMailshare extends IDirEntryPhotoSupport {

	/**
	 * Creates a new {@link Mailshare} entry.
	 * 
	 * @param uid       uid of the entry
	 * @param mailshare value of the entry
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, Mailshare mailshare) throws ServerFault;

	/**
	 * Creates a new {@link Mailshare} entry with a given uid.
	 * 
	 * @param item          uid of the entry
	 * @param mailshareItem item value of the entry
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}/createwithitem")
	public void createWithItem(@PathParam(value = "uid") String uid, ItemValue<Mailshare> mailshareItem)
			throws ServerFault;

	/**
	 * Modifies an existing {@link Mailshare} entry.
	 * 
	 * @param uid       uid of the entry
	 * @param mailshare value of the entry
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, Mailshare mailshare) throws ServerFault;

	/**
	 * Modifies an existing {@link Mailshare} entry
	 * 
	 * @param uid           uid of the entry
	 * @param mailshareItem item value of the entry
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/updatewithitem")
	public void updateWithItem(@PathParam(value = "uid") String uid, ItemValue<Mailshare> mailshareItem)
			throws ServerFault;

	/**
	 * Fetch a {@link Mailshare} from its unique uid
	 * 
	 * @param uid
	 * @return {@link ItemValue<Mailshare>}
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<Mailshare> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetches all Mailshares
	 * 
	 * @return list of all Mailshares
	 * @throws ServerFault
	 */
	@GET
	@Path("_complete")
	public List<ItemValue<Mailshare>> allComplete() throws ServerFault;

	/**
	 * Delete {@link Mailshare} entry
	 * 
	 * @param uid
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	public TaskRef delete(@PathParam(value = "uid") String uid) throws ServerFault;

}
