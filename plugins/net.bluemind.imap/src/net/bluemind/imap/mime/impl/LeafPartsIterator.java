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
package net.bluemind.imap.mime.impl;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.bluemind.imap.mime.MimePart;

/**
 * Used to iterate leaf parts of the MIME tree
 * 
 *
 */
public class LeafPartsIterator implements Iterator<MimePart> {

	private List<MimePart> l;
	private Iterator<MimePart> it;

	public LeafPartsIterator(MimePart mp) {
		l = new LinkedList<MimePart>();
		buildLeafList(mp);
		it = l.iterator();
	}

	private void buildLeafList(MimePart mp) {
		if (mp.getChildren().size() == 0) {
			l.add(mp);
		} else {
			for (MimePart m : mp.getChildren()) {
				buildLeafList(m);
			}
		}
	}

	@Override
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public MimePart next() {
		return it.next();
	}

	@Override
	public void remove() {
	}

}
