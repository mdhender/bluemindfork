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
package net.bluemind.mailflow.api;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.mailflow.common.api.Message;

@BMApi(version = "3")
@Path("/mailflow/{domainUid}")
public interface IMailflowRules extends IRestoreCrudSupport<MailRuleActionAssignmentDescriptor> {

	@PUT
	@Path("{uid}")
	public void create(@PathParam("uid") String uid, MailRuleActionAssignmentDescriptor assignment) throws ServerFault;

	@POST
	@Path("{uid}")
	public void update(@PathParam("uid") String uid, MailRuleActionAssignmentDescriptor assignment) throws ServerFault;

	@GET
	@Path("{uid}")
	public MailRuleActionAssignment getAssignment(@PathParam("uid") String uid) throws ServerFault;

	@DELETE
	@Path("{uid}")
	public void delete(@PathParam("uid") String uid) throws ServerFault;

	@GET
	@Path("_actions")
	public List<MailActionDescriptor> listActions() throws ServerFault;

	@GET
	@Path("_rules")
	public List<MailRuleDescriptor> listRules() throws ServerFault;

	@GET
	@Path("_assignments")
	public List<MailRuleActionAssignment> listAssignments() throws ServerFault;

	@POST
	@Path("_evaluation")
	public List<MailRuleActionAssignment> evaluate(Message message) throws ServerFault;

}
