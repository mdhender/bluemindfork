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
package net.bluemind.webappdata.service;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.webappdata.api.IWebAppData;
import net.bluemind.webappdata.api.WebAppData;

/**
 * 
 * WebAppData API.
 * 
 */
public class WebAppDataService implements IWebAppData {

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getVersion() throws ServerFault {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {
		// TODO Auto-generated method stub

	}

	@Override
	public Ack update(String uid, WebAppData value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Ack create(String uid, WebAppData value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(String uid) {
		// TODO Auto-generated method stub

	}

	@Override
	public ItemValue<WebAppData> getComplete(String uid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ItemValue<WebAppData>> multipleGet(List<String> uids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WebAppData get(String uid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void restore(ItemValue<WebAppData> item, boolean isCreate) {
		// TODO Auto-generated method stub

	}

	@Override
	public ItemValue<WebAppData> getCompleteById(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ItemValue<WebAppData>> multipleGetById(List<Long> ids) {
		// TODO Auto-generated method stub
		return null;
	}

}
