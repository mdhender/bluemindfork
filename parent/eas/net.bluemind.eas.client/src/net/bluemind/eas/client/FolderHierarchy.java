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
package net.bluemind.eas.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FolderHierarchy implements Map<FolderType, List<Folder>> {

	private Map<FolderType, List<Folder>> folders;

	public FolderHierarchy(Map<FolderType, List<Folder>> folders) {
		this.folders = new HashMap<FolderType, List<Folder>>(folders.size() + 1);
		this.folders.putAll(folders);
	}

	public int size() {
		return folders.size();
	}

	public boolean isEmpty() {
		return folders.isEmpty();
	}

	public boolean containsKey(Object key) {
		return folders.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return folders.containsValue(value);
	}

	public List<Folder> get(Object key) {
		return folders.get(key);
	}

	public List<Folder> put(FolderType key, List<Folder> value) {
		return folders.put(key, value);
	}

	public List<Folder> remove(Object key) {
		return folders.remove(key);
	}

	public void putAll(Map<? extends FolderType, ? extends List<Folder>> m) {
		folders.putAll(m);
	}

	public void clear() {
		folders.clear();
	}

	public Set<FolderType> keySet() {
		return folders.keySet();
	}

	public Collection<List<Folder>> values() {
		return folders.values();
	}

	public Set<java.util.Map.Entry<FolderType, List<Folder>>> entrySet() {
		return folders.entrySet();
	}

	public boolean equals(Object o) {
		return folders.equals(o);
	}

	public int hashCode() {
		return folders.hashCode();
	}

}
