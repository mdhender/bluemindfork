/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.webappdata.api;

import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICrudSupport;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.IReadByIdSupport;
import net.bluemind.core.container.api.IRestoreCrudSupport;

/**
 * 
 * WebAppData API - allow to save web applications data. All methods work on
 * {@link WebAppData} in a specific container identified by a unique UID, see
 * {@link WebAppData.getContainerUid}. Use
 * {@link net.bluemind.core.container.api.IContainers#all} to lookup all
 * containers of specific type.
 * 
 */
@BMApi(version = "3")
@Path("/webappdata/{containerUid}")
public interface IWebAppData extends IChangelogSupport, IDataShardSupport, ICrudSupport<WebAppData>,
		IRestoreCrudSupport<WebAppData>, IReadByIdSupport<WebAppData> {

}
