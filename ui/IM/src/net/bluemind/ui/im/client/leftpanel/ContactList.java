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
package net.bluemind.ui.im.client.leftpanel;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.ui.FlowPanel;

public class ContactList extends FlowPanel {

	private List<Entry> entries;

	public ContactList() {
		entries = new LinkedList<Entry>();
	}

	public void add(Entry e) {
		entries.add(e);
		reorder();
	}

	public void remove(Entry e) {
		entries.remove(e);
		super.remove(e);
	}

	private void sort() {
		// TODO: sort by status && display name ?
		Collections.sort(entries, new Comparator<Entry>() {

			@Override
			public int compare(Entry o1, Entry o2) {
				return o1.getFullName().toLowerCase().compareTo(o2.getFullName().toLowerCase());
			}
		});

	}

	public void reorder() {
		sort();
		clear();
		for (Entry entry : entries) {
			super.add(entry);
		}
	}
}