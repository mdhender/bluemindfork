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
package net.bluemind.dataprotect.api;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;

@BMApi(version = "3")
@Path("/dataprotect")
public interface IDataProtect {

	/**
	 * Returns all protected generations, ordered from oldest to most recent
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("generations")
	List<DataProtectGeneration> getAvailableGenerations() throws ServerFault;

	/**
	 * Loads an index of protected datas with possible restore actions.
	 * 
	 * @param dpg
	 * @return {@link GenerationContent} XML from {@link TaskStatus} result
	 * @throws ServerFault
	 */
	@POST
	@Path("_content/{partGen}")
	TaskRef getContent(@PathParam("partGen") String partGenerationId) throws ServerFault;

	/**
	 * Returns infos about the restore operations that the core provides
	 * 
	 * @param token
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("restore/_capabilities")
	List<RestoreOperation> getRestoreCapabilities() throws ServerFault;

	/**
	 * * Returns infos about the restore operations that the core provides
	 * (filtered by tags)
	 * 
	 * @param tags
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("restore/_capabilities_by_tags")
	List<RestoreOperation> getRestoreCapabilitiesByTags(List<String> tags) throws ServerFault;

	/**
	 * Executes a restore operation on the given {@link Restorable} item using
	 * data from a {@link DataProtectGeneration}
	 * 
	 * @param restoreDefinition
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("restore")
	// FIXME clean api after i understood parameters..
	TaskRef run(RestoreDefinition restoreDefinition) throws ServerFault;

	/**
	 * Removes protected data
	 * 
	 * @param dpg
	 * @return
	 * @throws ServerFault
	 */
	@DELETE
	@Path("generations")
	TaskRef forget(@QueryParam("generationId") int generationId) throws ServerFault;

	/**
	 * Returns the {@link RetentionPolicy} used for backup automatic removals.
	 * Never returns <code>null</code>.
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("policy")
	RetentionPolicy getRetentionPolicy() throws ServerFault;

	/**
	 * creates or updates the {@link RetentionPolicy}.
	 * 
	 * @param rp
	 * @throws ServerFault
	 */
	@POST
	@Path("policy")
	void updatePolicy(RetentionPolicy rp) throws ServerFault;

	/**
	 * Re-creates the in-database metadata using the generations.xml index from
	 * the data protect spool on the filesystem.
	 * 
	 * This is used for re-creating a blue mind server using backups. This must
	 * be used when /var/backups/bluemind becomes inconsistent with the database
	 * content (eg. after a TINA restore of /var/backups/bluemind)
	 * 
	 * @throws ServerFault
	 */
	@POST
	@Path("_syncfs")
	void syncWithFilesystem() throws ServerFault;

	/**
	 * Populates a blue mind database using protected data from a given
	 * generation.
	 * 
	 * @param at
	 * @param generationId
	 * @throws ServerFault
	 */
	@POST
	@Path("_install")
	TaskRef installFromGeneration(@QueryParam("generationId") int generationId) throws ServerFault;

	/**
	 * Run the incremental dataprotect backup process
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_backup")
	TaskRef saveAll() throws ServerFault;

}
