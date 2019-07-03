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
package net.bluemind.filehosting.filesystem.service.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileType;
import net.bluemind.filehosting.api.Metadata;
import net.bluemind.node.api.FileDescription;

public class FileHostingItemUtil {

	public static List<FileHostingItem> fromFileDescriptionList(List<FileDescription> list, File root) {
		List<FileHostingItem> items = new ArrayList<FileHostingItem>();
		for (FileDescription file : list) {
			items.add(fromFile(file, root));
		}
		return items;
	}

	public static FileHostingItem fromFile(FileDescription file, File root) {
		List<Metadata> metadata = new ArrayList<>();
		metadata.add(new Metadata("Content-Length", String.valueOf(file.getSize())));
		return new FileHostingItem(getRelativePath(file, root), file.getName(), getFileType(file), file.getSize(),
				metadata);
	}

	private static String getRelativePath(FileDescription file, File root) {
		int rootPathLength = root.getAbsolutePath().length();
		return file.getPath().substring(rootPathLength);
	}

	public static FileType getFileType(FileDescription file) {
		return file.isDirectory() ? FileType.DIRECTORY : FileType.FILE;
	}

}
