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
package net.bluemind.calendar.hook.ics;

import java.io.File;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.document.persistence.fs.FilesystemStore;
import net.bluemind.document.storage.IDocumentStore;

public class TestFileSystemStore extends FilesystemStore implements IDocumentStore {

	public TestFileSystemStore() {
		File file = new File(System.getProperty("java.io.tmpdir"), "bm-docs35");
		file.mkdirs();
		super.STORAGE_DIR = file.getAbsolutePath();
	}

	@Override
	public void store(String uid, byte[] content) throws ServerFault {
		super.store(uid, content);
	}

	@Override
	public byte[] get(String uid) throws ServerFault {
		return super.get(uid);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean exists(String uid) throws ServerFault {
		return false;
	}

	@Override
	public int getPriority() {
		return 2;
	}

}
