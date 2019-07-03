package net.bluemind.mailbox.api;

import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.IDirEntryMaintenance;

@BMApi(version = "3")
@Path("/mailboxes/{domainUid}")
public interface IMailboxes {
	/**
	 * Creates a new {@link Mailbox} entry.
	 * 
	 * @param uid
	 *                    uid of the entry
	 * @param mailbox
	 *                    value of the entry
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, Mailbox mailbox) throws ServerFault;

	/**
	 * Modifies an existing {@link Mailbox} entry.
	 * 
	 * @param uid
	 *                    uid of the entry
	 * @param mailbox
	 *                    value of the entry
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, Mailbox mailbox) throws ServerFault;

	/**
	 * Fetch a {@link Mailbox} from its unique uid
	 * 
	 * @param uid
	 * @return {@link ItemValue<Mailshare>}
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/complete")
	public ItemValue<Mailbox> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Get the unread mail messages count of the currently logged in user
	 * 
	 * @return unread messages count
	 * @throws ServerFault
	 */
	@GET
	@Path("_unread")
	public Integer getUnreadMessagesCount() throws ServerFault;

	@GET
	@Path("{uid}/_quota")
	public MailboxQuota getMailboxQuota(@PathParam(value = "uid") String uid) throws ServerFault;

	@GET
	@Path("{uid}/_config")
	public MailboxConfig getMailboxConfig(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Delete {@link Mailbox} entry
	 * 
	 * @param uid
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	public void delete(@PathParam(value = "uid") String uid) throws ServerFault;

	@GET
	@Path("_byemail")
	public ItemValue<Mailbox> byEmail(@QueryParam("email") String email) throws ServerFault;

	@GET
	@Path("_byname")
	public ItemValue<Mailbox> byName(@QueryParam("name") String name) throws ServerFault;

	@GET
	@Path("_filter")
	public MailFilter getDomainFilter() throws ServerFault;

	@POST
	@Path("_filter")
	public void setDomainFilter(MailFilter filter) throws ServerFault;

	@GET
	@Path("_byType")
	public List<String> byType(@QueryParam("email") Mailbox.Type type) throws ServerFault;

	@GET
	@Path("{mailboxUid}/_filter")
	public MailFilter getMailboxFilter(@PathParam("mailboxUid") String mailboxUid) throws ServerFault;

	@POST
	@Path("{mailboxUid}/_filter")
	public void setMailboxFilter(@PathParam("mailboxUid") String mailboxUid, MailFilter filter) throws ServerFault;

	@GET
	@Path("{mailboxUid}/_acls")
	public List<AccessControlEntry> getMailboxAccessControlList(@PathParam("mailboxUid") String mailboxUid)
			throws ServerFault;

	@POST
	@Path("{mailboxUid}/_acls")
	public void setMailboxAccessControlList(@PathParam("mailboxUid") String mailboxUid,
			List<AccessControlEntry> accessControlEntries) throws ServerFault;

	@GET
	@Path("_list")
	public List<ItemValue<Mailbox>> list() throws ServerFault;

	@GET
	@Path("_listUids")
	public List<String> listUids();

	@GET
	@Path("_byRouting")
	public List<String> byRouting(@QueryParam("email") Mailbox.Routing routing) throws ServerFault;

	/**
	 * Use {@link IDirEntryMaintenance#repair(Set)}
	 * 
	 * <pre>
	 * <code>
	 * {@code Set<String>} opsIds = IDirEntryMaintenance.getAvailableOperations()
	 * 					.stream().map(mo -> mo.identifier)
	 * 					.collect(Collectors.toSet());
	 * 
	 * for (String entryUid: IDirectory.search(
	 * 			DirEntryQuery.filterKind(Kind.GROUP, Kind.MAILSHARE, Kind.RESOURCE, Kind.USER))
	 * 		.values.stream()
	 * 		.map(deiv -> deiv.uid)
	 * 		.collect()Collectors.toSet()) {
	 * 	IDirEntryMaintenance.repair(opsIds);
	 * }
	 * </code>
	 * </pre>
	 */
	@Deprecated
	@POST
	@Path("_check-and-repair-all")
	public TaskRef checkAndRepairAll() throws ServerFault;

	/**
	 * Use {@link IDirEntryMaintenance#check(Set)}
	 * 
	 * <pre>
	 * <code>
	 * {@code Set<String>} opsIds = IDirEntryMaintenance.getAvailableOperations()
	 * 					.stream().map(mo -> mo.identifier)
	 * 					.collect(Collectors.toSet());
	 * 
	 * for (String entryUid: IDirectory.search(
	 * 			DirEntryQuery.filterKind(Kind.GROUP, Kind.MAILSHARE, Kind.RESOURCE, Kind.USER))
	 * 		.values.stream()
	 * 		.map(deiv -> deiv.uid)
	 * 		.collect()Collectors.toSet()) {
	 * 	IDirEntryMaintenance.check(opsIds);
	 * }
	 * </code>
	 * </pre>
	 */
	@Deprecated
	@POST
	@Path("_check-all")
	public TaskRef checkAll() throws ServerFault;

	/**
	 * Use {@link IDirEntryMaintenance#repair(Set)}
	 * 
	 * <pre>
	 * <code>
	 * {@code Set<String>} opsIds = IDirEntryMaintenance.getAvailableOperations()
	 * 					.stream().map(mo -> mo.identifier)
	 * 					.collect(Collectors.toSet());
	 * 
	 * 	IDirEntryMaintenance.repair(opsIds);
	 * </code>
	 * </pre>
	 */
	@Deprecated
	@POST
	@Path("{uid}/_check-and-repair")
	public TaskRef checkAndRepair(@PathParam("uid") String uid) throws ServerFault;

	/**
	 * Use {@link IDirEntryMaintenance#check(Set)}
	 * 
	 * <pre>
	 * <code>
	 * {@code Set<String>} opsIds = IDirEntryMaintenance.getAvailableOperations()
	 * 					.stream().map(mo -> mo.identifier)
	 * 					.collect(Collectors.toSet());
	 * 
	 * IDirEntryMaintenance.check(opsIds);
	 * </code>
	 * </pre>
	 */
	@Deprecated
	@POST
	@Path("{uid}/_check")
	public TaskRef check(@PathParam("uid") String uid) throws ServerFault;

	@POST
	@Path("_mget")
	public List<ItemValue<Mailbox>> multipleGet(List<String> uids) throws ServerFault;

}
