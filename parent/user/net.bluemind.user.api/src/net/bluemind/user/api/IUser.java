package net.bluemind.user.api;

import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.IDirEntryExtIdSupport;
import net.bluemind.directory.api.IDirEntryPhotoSupport;
import net.bluemind.group.api.IGroupMember;

/**
 * {@link User} API. {domainUid} corresponds to the fully qualified domain name
 *
 */
@BMApi(version = "3")
@Path("/users/{domainUid}")
public interface IUser
		extends IDirEntryPhotoSupport, IDirEntryExtIdSupport, IRestoreDirEntryWithMailboxSupport<User>, IGroupMember {

	/**
	 * Creates a new {@link User} with the given uid. Also creates default calendar,
	 * todo elements and the {@link User}'s mailbox if routing is internal
	 * 
	 * @param uid  the user's unique id
	 * @param user user data
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, User user) throws ServerFault;

	/**
	 * Creates a new {@link User} with the given uid. Associates an external id to
	 * the {@link User}
	 * 
	 * @param uid   the user's unique id
	 * @param extId an external id. Usually used to link the user to an external
	 *              system
	 * @param user  user data
	 * @throws ServerFault standard error object
	 */
	@PUT
	@Path("{uid}/{extid}/createwithextid")
	public void createWithExtId(@PathParam(value = "uid") String uid, @PathParam(value = "extid") String extId,
			User user) throws ServerFault;

	/**
	 * Modifies an existing {@link User}
	 * 
	 * @param uid  the user's unique id
	 * @param user the new user values
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, User user) throws ServerFault;

	/**
	 * Fetches a {@link User} by its unique id
	 * 
	 * @param uid the user's unique id
	 * @return the user item value, or null if the user does not exist
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<User> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Fetches a {@link User} by its email
	 * 
	 * @param email the user's email
	 * @return the user item value, or null if the user does not exist
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("byEmail/{email}")
	public ItemValue<User> byEmail(@PathParam(value = "email") String email) throws ServerFault;

	/**
	 * Fetches a {@link User} by its external id
	 * 
	 * @param extId the user's external id
	 * @return the user item value, or null if the user does not exist
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("byExtId/{extid}")
	public ItemValue<User> byExtId(@PathParam(value = "extid") String extId) throws ServerFault;

	/**
	 * Fetches a {@link User} by its login
	 * 
	 * @param login the user's login
	 * @return the user item value, or null if the user does not exist
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("byLogin/{login}")
	public ItemValue<User> byLogin(@PathParam(value = "login") String login) throws ServerFault;

	/**
	 * Deletes a {@link User}. Also deletes all user related objects and the user's
	 * mailbox
	 * 
	 * @param uid the user's unique id
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@DELETE
	@Path("{uid}")
	public TaskRef delete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Retrieve all existing {@link User} uids
	 * 
	 * @return a list of all existing {@link User} uids
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("_alluids")
	public List<String> allUids() throws ServerFault;

	/**
	 * Sets the {@link User}'s roles. Replaces all existing role assignments
	 * 
	 * @param uid   the user's unique id
	 * @param roles a set of roles to be assigned to the user
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@POST
	@Path("{uid}/roles")
	public void setRoles(@PathParam(value = "uid") String uid, Set<String> roles) throws ServerFault;

	/**
	 * Retrieves a list of all {@link User}s owning all of the provided roles
	 * 
	 * @param roles a list of roles
	 * @return a list of users owning all provided roles
	 * @throws ServerFault standard error object (unchecked exception)
	 * @deprecated This method may return group entities as well. Use
	 *             {@link net.bluemind.directory.api.IDirectory#getByRoles(List)}
	 *             instead
	 */
	@Deprecated
	@POST
	@Path("_roleusers")
	Set<String> getUsersWithRoles(List<String> roles) throws ServerFault;

	/**
	 * Retrieves a list of all roles owned by a {@link User}. Also includes roles
	 * indirectly assigned to the {@link User}, for example by its
	 * {@link net.bluemind.group.api.Group} memberships
	 * 
	 * @param uid the user's unique id
	 * @return a list of roles
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{uid}/roles_resolved")
	public Set<String> getResolvedRoles(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Retrieves a list of all roles directly owned by a {@link User}
	 * 
	 * @param the user's unique id
	 * @return a list of roles
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{uid}/roles")
	public Set<String> getRoles(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Update a {@link User}'s password
	 * 
	 * @param uid      user's unique id
	 * @param password object containing the current and new password
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@POST
	@Path("{uid}/password_")
	public void setPassword(@PathParam(value = "uid") String uid, ChangePassword password) throws ServerFault;

	/**
	 * Retrieves a {@link User}'s icon/avatar
	 * 
	 * @param uid user's unique id
	 * @return a byte array containing an icon in png format
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{uid}/icon")
	@Produces("image/png")
	public byte[] getIcon(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Updates a {@link User}'s contact informations
	 * 
	 * @param uid       user's unique id
	 * @param userVCard the new contact informations
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@POST
	@Path("{uid}/vcard")
	public void updateVCard(@PathParam(value = "uid") String uid, VCard userVCard) throws ServerFault;

	/**
	 * Retrieves a {@link User}'s contact informations
	 * ({@link net.bluemind.addressbook.api.VCard})
	 * 
	 * @param uid user's unique id
	 * @return the user's contact informations
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@GET
	@Path("{uid}/vcard")
	public VCard getVCard(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Modifies a {@link User}'s {@link net.bluemind.directory.api.AccountType}
	 * 
	 * @param uid         user's unique id
	 * @param accountType the new account type
	 * @throws ServerFault standard error object (unchecked exception)
	 */
	@POST
	@Path("_updateAccountType/{uid}")
	public void updateAccountType(@PathParam("uid") String uid, AccountType accountType) throws ServerFault;

}
