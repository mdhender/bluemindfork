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

package net.bluemind.dataprotect.service;

import java.util.Map;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.server.api.Server;

public interface IBackupWorker {

	String getDataType();

	boolean supportsTag(String tag);

	/**
	 * This is called before the backup starts. This phase is used to put the
	 * data in a backup-able state.
	 * 
	 * For a database it means creating a dump, for a cyrus mail server it means
	 * stopping it, etc.
	 * 
	 * @param ctx
	 * @param tag
	 * @param toBackup
	 * @throws ServerFault
	 */
	void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault;

	/**
	 * This is called after <code>prepareDataDirs</code>. Rsync will be used to
	 * save those dirs. Symlinks are handled by the dataprotect code, so you
	 * don't need special code here to check if the dir war symlinked elsewhere.
	 * 
	 * @return the dirs that need to be data protected
	 */
	Set<String> getDataDirs();

	/**
	 * This gets called once the data is saved with rsync. At this point you can
	 * remove the data you exported for backup, restart the services that you
	 * stopped, etc.
	 * 
	 * @param ctx
	 * @param tag
	 * @param backedUp
	 * @throws ServerFault
	 */
	void dataDirsSaved(IDPContext ctx, String tag, ItemValue<Server> backedUp) throws ServerFault;

	public void restore(IDPContext ctx, PartGeneration part, Map<String, Object> params) throws ServerFault;

	public void cleanup(IDPContext ctx, PartGeneration part, Map<String, Object> params) throws ServerFault;

}
