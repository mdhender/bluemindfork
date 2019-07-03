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
package net.bluemind.authentication.api;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

/**
 * Authentication service
 *
 */
@BMApi(version = "3")
@Path("/auth")
public interface IAuthentication {

	/**
	 * Try to log in user into Blue-Mind and create valid session on success
	 * 
	 * @param user
	 *            user login
	 * @param password
	 *            user password
	 * @param origin
	 *            Blue Mind application from which user try to log in
	 * @return {@link LoginResponse}
	 * @throws ServerFault
	 */
	@POST
	@Path("login")
	public LoginResponse login(@QueryParam("login") String login, String password, @QueryParam("origin") String origin)
			throws ServerFault;

	/**
	 * Try to log in user into Blue-Mind and create valid session on success
	 * 
	 * @param user
	 *            user login
	 * @param password
	 *            user password
	 * @param origin
	 *            Blue Mind application from which user try to log in
	 * @param interactive
	 *            interactive
	 * @return {@link LoginResponse}
	 * @throws ServerFault
	 */
	@POST
	@Path("loginWithParams")
	public LoginResponse loginWithParams(@QueryParam("login") String login, String password,
			@QueryParam("origin") String origin, @QueryParam("interactive") Boolean interactive) throws ServerFault;

	/**
	 * Validate credential
	 * 
	 * @param user
	 *            user login
	 * @param password
	 *            user password
	 * @param origin
	 *            Blue Mind application from which user try to log in
	 * @return {@link LoginResponse}
	 * @throws ServerFault
	 */
	@POST
	@Path("validate")
	public ValidationKind validate(@QueryParam("login") String login, String password,
			@QueryParam("origin") String origin) throws ServerFault;

	/**
	 * Refreshes the session, or throws exception if the SecurityContext is
	 * invalid.
	 * 
	 * @throws ServerFault
	 */
	@GET
	@Path("ping")
	public void ping() throws ServerFault;

	/**
	 * Close a Blue-Mind session
	 * 
	 */
	@POST
	@Path("logout")
	public void logout() throws ServerFault;

	/**
	 * Create Blue-Mind access token for requested user.<br>
	 * This token can be used for authenticate against Blue-Mind components
	 * <p>
	 * Only token from global domain are allowed to do this.
	 * 
	 * @param login
	 *            requested login@domain access token
	 * @return {@link LoginResponse}
	 * @throws ServerFault
	 */
	@POST
	@Path("_su")
	public LoginResponse su(@QueryParam("login") String login) throws ServerFault;

	@POST
	@Path("_suWithParams")
	public LoginResponse suWithParams(@QueryParam("login") String login, @QueryParam("interactive") Boolean interactive)
			throws ServerFault;

	@GET
	public AuthUser getCurrentUser() throws ServerFault;
}
