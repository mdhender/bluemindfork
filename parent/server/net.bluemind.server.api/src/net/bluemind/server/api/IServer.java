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

package net.bluemind.server.api;

import java.util.List;

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
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;

/**
 * API for managing the servers in a BlueMind installation.
 * 
 * {@link Server} is the equivalent of Host in core_v1.
 * 
 * Server are items living in an <code>installation</code> Container. The uid of
 * the container is determined using the /etc/bm/mcast.id on your bluemind
 * installation.
 * 
 */
@BMApi(version = "3")
@Path("/servers/{containerUid}")
public interface IServer {

	/**
	 * Creates a new server in the database with the given uid. IServerHook
	 * implementations are invoked.
	 * 
	 * @param uid
	 * @param srv
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}")
	TaskRef create(@PathParam(value = "uid") String uid, Server srv) throws ServerFault;

	/**
	 * Updates a server in the database with its uid. IServerHook
	 * implementations are invoked.
	 * 
	 * @param uid
	 * @param srv
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}")
	TaskRef update(@PathParam(value = "uid") String uid, Server srv) throws ServerFault;

	/**
	 * Fetches a server object from the database with its uid.
	 * 
	 * @param uid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/complete")
	ItemValue<Server> getComplete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Deletes a server object in the database with its uid.
	 * 
	 * @param uid
	 * @throws ServerFault
	 */
	@DELETE
	@Path("{uid}")
	void delete(@PathParam(value = "uid") String uid) throws ServerFault;

	/**
	 * Enumerate all the servers in the installation container.
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_complete")
	public List<ItemValue<Server>> allComplete() throws ServerFault;

	/**
	 * Executes a command using bm-node on the server with the given uid.
	 * Returns a reference to the running command that must be used in
	 * subsequent <code>getStatus</code> calls.
	 * 
	 * @param uid
	 * @param command
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/submit_command")
	String submit(@PathParam(value = "uid") String uid, String command) throws ServerFault;

	/**
	 * Executes a command using bm-node on the server with the given uid.
	 * Returns command execution output and exit code ({@link CommandStatus}).
	 * 
	 * @param uid
	 * @param command
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/submit_command_and_wait")
	CommandStatus submitAndWait(@PathParam(value = "uid") String uid, String command) throws ServerFault;

	/**
	 * Fetches the progress of a command running in bm-node
	 * 
	 * @param uid
	 * @param commandRef
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/command_status")
	CommandStatus getStatus(@PathParam(value = "uid") String uid, @QueryParam(value = "ref") String commandRef)
			throws ServerFault;

	/**
	 * Uses bm-node to read a file on a {@link Server} with its uid.
	 * 
	 * @param uid
	 *            the server uid
	 * @param path
	 *            the absolute filename to read
	 * @return the bytes in the file
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/fs/{path}")
	byte[] readFile(@PathParam(value = "uid") String uid, @PathParam(value = "path") String path) throws ServerFault;

	/**
	 * Uses bm-node to write a file on a {@link Server} with its uid.
	 * 
	 * @param uid
	 *            the server uid
	 * @param path
	 *            the absolute filename to write
	 * @param content
	 * @throws ServerFault
	 */
	@PUT
	@Path("{uid}/fs/{path}")
	void writeFile(@PathParam(value = "uid") String uid, @PathParam(value = "path") String path, byte[] content)
			throws ServerFault;

	// FIXME should return a TaskRef
	/**
	 * Assigns a Server's tag to a domain. For example, when you assign your
	 * mail/imap Server to a domain blue-mind.net a mail partition will be
	 * created to hold this domain data.
	 * 
	 * This method stores the newly made assignment in the database and
	 * IServerHook implementations will do all the system work.
	 * 
	 * @param serverUid
	 * @param domainUid
	 * @param tag
	 * @throws ServerFault
	 */
	@POST
	@Path("{domainUid}/assignments/{serverUid}/_assign")
	void assign(@PathParam(value = "serverUid") String serverUid, @PathParam(value = "domainUid") String domainUid,
			@QueryParam(value = "tag") String tag) throws ServerFault;

	// FIXME should return a TaskRef
	/**
	 * Undo what assign does.
	 * 
	 * @param serverUid
	 * @param domainUid
	 * @param tag
	 * @throws ServerFault
	 */
	@POST
	@Path("{domainUid}/assignments/{serverUid}/_unassign")
	void unassign(@PathParam(value = "serverUid") String serverUid, @PathParam(value = "domainUid") String domainUid,
			@QueryParam(value = "tag") String tag) throws ServerFault;

	/**
	 * Fetches all the server assignments in a domain container
	 * 
	 * @param domainUid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{domainUid}/assignments")
	List<Assignment> getAssignments(@PathParam(value = "domainUid") String domainUid) throws ServerFault;

	/**
	 * Fetches all the servers in a domain container by its assignment
	 * 
	 * @param domainUid
	 * @param assignment
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("{domainUid}/byAssignment")
	List<String> byAssignment(@PathParam(value = "domainUid") String domainUid, @QueryParam(value = "tag") String tag)
			throws ServerFault;

	/**
	 * tags server and return ref to Task ({@link ITask}
	 * 
	 * @param uid
	 * @param tags
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/tags")
	TaskRef setTags(@PathParam(value = "uid") String uid, List<String> tags) throws ServerFault;

	@GET
	@Path("{uid}/serverAssignments")
	List<Assignment> getServerAssignments(@PathParam(value = "uid") String uid) throws ServerFault;

}
