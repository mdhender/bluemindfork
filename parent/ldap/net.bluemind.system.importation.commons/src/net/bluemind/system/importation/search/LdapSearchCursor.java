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
package net.bluemind.system.importation.search;

import java.io.IOException;
import java.util.Iterator;

import org.apache.directory.api.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.IntermediateResponse;
import org.apache.directory.api.ldap.model.message.Referral;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchResultDone;

public class LdapSearchCursor implements AutoCloseable, SearchCursor {
	private final SearchCursor cursor;

	public LdapSearchCursor(SearchCursor cursor) {
		this.cursor = cursor;
	}

	@Override
	public void close() {
		if (cursor != null) {
			try {
				cursor.close();
			} catch (IOException e) {
				// that's ok
			}
		}
	}

	@Override
	public void after(Response arg0) throws LdapException, CursorException {
		cursor.after(arg0);
	}

	@Override
	public void afterLast() throws LdapException, CursorException {
		cursor.afterLast();
	}

	@Override
	public boolean available() {
		return cursor.available();
	}

	@Override
	public void before(Response arg0) throws LdapException, CursorException {
		cursor.before(arg0);
	}

	@Override
	public void beforeFirst() throws LdapException, CursorException {
		cursor.beforeFirst();
	}

	@Override
	public void close(Exception arg0) throws IOException {
		cursor.close(arg0);
	}

	@Override
	public boolean first() throws LdapException, CursorException {
		return cursor.first();
	}

	@Override
	public Response get() throws CursorException {
		return cursor.get();
	}

	@Override
	public boolean isAfterLast() {
		return cursor.isAfterLast();
	}

	@Override
	public boolean isBeforeFirst() {
		return cursor.isBeforeFirst();
	}

	@Override
	public boolean isClosed() {
		return cursor.isClosed();
	}

	@Override
	public boolean isFirst() {
		return cursor.isFirst();
	}

	@Override
	public boolean isLast() {
		return cursor.isLast();
	}

	@Override
	public boolean last() throws LdapException, CursorException {
		return cursor.last();
	}

	@Override
	public boolean next() throws LdapException, CursorException {
		return cursor.next();
	}

	@Override
	public boolean previous() throws LdapException, CursorException {
		return cursor.previous();
	}

	@Override
	public void setClosureMonitor(ClosureMonitor arg0) {
		cursor.setClosureMonitor(arg0);
	}

	@Override
	public String toString(String arg0) {
		return cursor.toString();
	}

	@Override
	public Iterator<Response> iterator() {
		return cursor.iterator();
	}

	@Override
	public Entry getEntry() throws LdapException {
		return cursor.getEntry();
	}

	@Override
	public IntermediateResponse getIntermediate() throws LdapException {
		return cursor.getIntermediate();
	}

	@Override
	public Referral getReferral() throws LdapException {
		return cursor.getReferral();
	}

	@Override
	public SearchResultDone getSearchResultDone() {
		return cursor.getSearchResultDone();
	}

	@Override
	public boolean isDone() {
		return cursor.isDone();
	}

	@Override
	public boolean isEntry() {
		return cursor.isEntry();
	}

	@Override
	public boolean isIntermediate() {
		return cursor.isIntermediate();
	}

	@Override
	public boolean isReferral() {
		return cursor.isReferral();
	}
}
