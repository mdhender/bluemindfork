package net.bluemind.mailbox.api;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.mailbox.api.rules.DelegationRule;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.RuleMoveDirection;
import net.bluemind.mailbox.api.rules.RuleMoveRelativePosition;

@BMApi(version = "3")
@Path("/mailboxes/{domainUid}")
public interface IMailboxes {
	/**
	 * Creates a new {@link Mailbox} entry.
	 * 
	 * @param uid     uid of the entry
	 * @param mailbox value of the entry
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, Mailbox mailbox) throws ServerFault;

	/**
	 * Modifies an existing {@link Mailbox} entry.
	 * 
	 * @param uid     uid of the entry
	 * @param mailbox value of the entry
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
	@Path("_rules")
	List<MailFilterRule> getDomainRules() throws ServerFault;

	@GET
	@Path("_rules/{id}")
	MailFilterRule getDomainRule(@PathParam("id") long id) throws ServerFault;

	@PUT
	@Path("_rules")
	Long addDomainRule(MailFilterRule rule) throws ServerFault;

	@POST
	@Path("_rules/{id}")
	void updateDomainRule(@PathParam("id") long id, MailFilterRule rule) throws ServerFault;

	@DELETE
	@Path("_rules/{id}")
	void deleteDomainRule(@PathParam("id") long id) throws ServerFault;

	@GET
	@Path("_byType")
	public List<String> byType(@QueryParam("email") Mailbox.Type type) throws ServerFault;

	@GET
	@Path("{mailboxUid}/_vacation")
	MailFilter.Vacation getMailboxVacation(@PathParam("mailboxUid") String mailboxUid);

	@POST
	@Path("{mailboxUid}/_vacation")
	public void setMailboxVacation(@PathParam("mailboxUid") String mailboxUid, MailFilter.Vacation vacation)
			throws ServerFault;

	@GET
	@Path("{mailboxUid}/_forwarding")
	MailFilter.Forwarding getMailboxForwarding(@PathParam("mailboxUid") String mailboxUid);

	@POST
	@Path("{mailboxUid}/_forwarding")
	public void setMailboxForwarding(@PathParam("mailboxUid") String mailboxUid, MailFilter.Forwarding forwarding)
			throws ServerFault;

	@GET
	@Path("{mailboxUid}/_filter")
	public MailFilter getMailboxFilter(@PathParam("mailboxUid") String mailboxUid) throws ServerFault;

	@POST
	@Path("{mailboxUid}/_filter")
	public void setMailboxFilter(@PathParam("mailboxUid") String mailboxUid, MailFilter filter) throws ServerFault;

	@GET
	@Path("{mailboxUid}/_rules")
	List<MailFilterRule> getMailboxRules(@PathParam("mailboxUid") String mailboxUid) throws ServerFault;

	@GET
	@Path("{mailboxUid}/_rulesByClient")
	List<MailFilterRule> getMailboxRulesByClient(@PathParam("mailboxUid") String mailboxUid,
			@QueryParam("client") String client) throws ServerFault;

	@GET
	@Path("{mailboxUid}/_rules/{id}")
	MailFilterRule getMailboxRule(@PathParam("mailboxUid") String mailboxUid, @PathParam("id") long id)
			throws ServerFault;

	@PUT
	@Path("{mailboxUid}/_rules")
	Long addMailboxRule(@PathParam("mailboxUid") String mailboxUid, MailFilterRule rule) throws ServerFault;

	@PUT
	@Path("{mailboxUid}/_rules/{position}/{anchorId}")
	Long addMailboxRuleRelative(@PathParam("mailboxUid") String mailboxUid,
			@PathParam("position") RuleMoveRelativePosition position, @PathParam("anchorId") long anchorId,
			MailFilterRule rule) throws ServerFault;

	@POST
	@Path("{mailboxUid}/_rules/{id}")
	void updateMailboxRule(@PathParam("mailboxUid") String mailboxUid, @PathParam("id") long id, MailFilterRule rule)
			throws ServerFault;

	@DELETE
	@Path("{mailboxUid}/_rules/{id}")
	void deleteMailboxRule(@PathParam("mailboxUid") String mailboxUid, @PathParam("id") long id) throws ServerFault;

	@POST
	@Path("{mailboxUid}/_rules/{id}/{direction}")
	void moveMailboxRule(@PathParam("mailboxUid") String mailboxUid, @PathParam("id") long id,
			@PathParam("direction") RuleMoveDirection direction) throws ServerFault;

	@POST
	@Path("{mailboxUid}/_rules/{id}/{position}/{anchorId}")
	void moveMailboxRuleRelative(@PathParam("mailboxUid") String mailboxUid, @PathParam("id") long id,
			@PathParam("position") RuleMoveRelativePosition position, @PathParam("anchorId") long anchorId)
			throws ServerFault;

	@POST
	@Path("{mailboxUid}/_delegationRule")
	public void setMailboxDelegationRule(@PathParam("mailboxUid") String mailboxUid, DelegationRule delegationRule)
			throws ServerFault;

	@GET
	@Path("{mailboxUid}/_delegationRule")
	DelegationRule getMailboxDelegationRule(@PathParam("mailboxUid") String mailboxUid) throws ServerFault;

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

	@POST
	@Path("_mget")
	public List<ItemValue<Mailbox>> multipleGet(List<String> uids) throws ServerFault;

}
