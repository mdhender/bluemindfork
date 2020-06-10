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
package net.bluemind.filehosting.service.export;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileHostingPublicLink;

public interface IFileHostingService {

	public default boolean isDefaultImplementation() {
		return false;
	}

	/**
	 * Checks, if filehosting operations are available using the provided context
	 * 
	 * @param context
	 * @return
	 */
	public default boolean supports(SecurityContext context) {
		return true;
	}

	/**
	 * Lists files and folders. The listing contains only non-recursive items
	 * 
	 * @param path the folder path
	 * @return the files and folders found under this path
	 * @throws ServerFault
	 */
	public List<FileHostingItem> list(SecurityContext context, String path) throws ServerFault;

	/**
	 * Finds items in the file hosting repository
	 * 
	 * @param query the query. The format of the query is repository dependent
	 * @return all items matching the query
	 * @throws ServerFault
	 */
	public List<FileHostingItem> find(SecurityContext context, String query) throws ServerFault;

	/**
	 * Retrieves a document from the file hosting repository
	 * 
	 * @param path the relative path to the document
	 * @return the document data
	 * @throws ServerFault
	 */
	public Stream get(SecurityContext context, String path) throws ServerFault;

	/**
	 * Checks if a file exists
	 * 
	 * @param path the relative path to the document
	 * @return true if the file exists, false otherwise
	 * @throws ServerFault common error object
	 */
	public boolean exists(SecurityContext context, String path) throws ServerFault;

	/**
	 * Retrieves a public URL to the document in the file hosting repository
	 * 
	 * @param path           the relative path to the document
	 * @param downloadLimit  the number of times the file can be downloaded, <= 0 if
	 *                       unlimited
	 * @param expirationDate a ISO-8601 compliant date, null otherwise
	 * 
	 * @return the URL pointing to this document
	 * @throws ServerFault
	 */
	public FileHostingPublicLink share(SecurityContext context, String path, Integer downloadLimit,
			String expirationDate) throws ServerFault;

	/**
	 * Remove a public link
	 * 
	 * @param context the securityContext
	 * @param url     the share url
	 * @throws ServerFault
	 */
	public void unShare(SecurityContext context, String url) throws ServerFault;

	/**
	 * Update/insert a document
	 * 
	 * @param path     the relative path in the file hosting repository
	 * @param document the document data
	 * @throws ServerFault
	 */
	public void store(SecurityContext context, String path, Stream document) throws ServerFault;

	/**
	 * Deletes a document
	 * 
	 * @param path the relative path in the file hosting repository
	 * @throws ServerFault
	 */
	public void delete(SecurityContext context, String path) throws ServerFault;

	/**
	 * Retrieves an entity from the file hosting repository
	 * 
	 * @param uid the entity uid
	 * @return the document data
	 * @throws ServerFault
	 */
	@Deprecated
	public FileHostingItem getComplete(SecurityContext context, String uid) throws ServerFault;

	/**
	 * Retrieves a document from the file hosting repository by its public uid
	 * 
	 * @param uid the document uid
	 * @return the document data
	 * @throws ServerFault
	 */
	@GET
	@Path("{path}/_public")
	@Deprecated
	public Stream getSharedFile(SecurityContext context, String uid) throws ServerFault;

	/**
	 * Retrieves informations about the filehosting implementation
	 * 
	 * @throws ServerFault
	 */
	@GET
	@Path("_info")
	public FileHostingInfo info(SecurityContext context) throws ServerFault;

}
