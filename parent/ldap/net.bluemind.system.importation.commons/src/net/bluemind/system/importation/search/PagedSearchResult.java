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

import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsDecorator;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.LdapResult;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.controls.PagedResults;
import org.apache.directory.ldap.client.api.LdapConnection;

public class PagedSearchResult implements Iterable<Response>, AutoCloseable {
	@SuppressWarnings("serial")
	public class LdapSearchException extends Exception {
		private final LdapResult ldapResult;

		public LdapSearchException(LdapResult ldapResult) {
			super(String.format("%s - %s", ldapResult.getResultCode(), ldapResult.getDiagnosticMessage()));
			this.ldapResult = ldapResult;
		}

		public LdapResult getLdapResult() {
			return ldapResult;
		}
	}

	public class PagedSearchIterator implements Iterator<Response> {
		private PagedSearchResult cursor;
		private boolean available;

		public PagedSearchIterator(PagedSearchResult cursor) {
			this.cursor = cursor;

			try {
				this.available = cursor.next();
			} catch (Exception e) {
				this.available = false;
			}
		}

		@Override
		public boolean hasNext() {
			return available;
		}

		@Override
		public Response next() {
			try {
				Response response = cursor.get();
				available = cursor.next();
				return response;
			} catch (Exception e) {
				throw new RuntimeException(I18n.err(I18n.ERR_02002_FAILURE_ON_UNDERLYING_CURSOR), e);
			}
		}

	}

	private final int pageSize;
	private final LdapConnection ldapCon;
	private final SearchRequest searchRequest;
	private PagedResults pagedSearchControl;
	private SearchCursor cursor = null;

	public PagedSearchResult(LdapConnection ldapCon, SearchRequest searchRequest, int pageSize) {
		this.ldapCon = ldapCon;
		this.searchRequest = searchRequest;
		this.pageSize = pageSize;
		pagedSearchControl = new PagedResultsDecorator(ldapCon.getCodecService());
	}

	public PagedSearchResult(LdapConnection ldapCon, SearchRequest searchRequest) {
		this.ldapCon = ldapCon;
		this.searchRequest = searchRequest;
		this.pageSize = 100;
		pagedSearchControl = new PagedResultsDecorator(ldapCon.getCodecService());
	}

	@Override
	public void close() {
		closeCursor();
	}

	private void closeCursor() {
		if (null != cursor) {
			try {
				cursor.close();
			} catch (IOException e) {
				// that's ok
			}
		}
	}

	public boolean next() throws LdapException, CursorException, LdapSearchException {
		if (cursor == null) {
			return getNextPage().next();
		}

		if (cursor.next()) {
			return true;
		}

		if (!hasNextPage()) {
			return false;
		}

		return getNextPage().next();
	}

	private SearchCursor getNextPage() throws LdapException, LdapSearchException {
		pagedSearchControl.setSize(pageSize);
		searchRequest.addControl(pagedSearchControl);

		closeCursor();

		cursor = ldapCon.search(searchRequest);

		SearchResultDone result = cursor.getSearchResultDone();
		LdapResult ldapResult = result.getLdapResult();
		if (ldapResult.getResultCode() != ResultCodeEnum.SUCCESS) {
			throw new LdapSearchException(ldapResult);
		}

		return cursor;
	}

	private boolean hasNextPage() throws LdapSearchException {
		SearchResultDone result = cursor.getSearchResultDone();
		LdapResult ldapResult = result.getLdapResult();
		if (ldapResult.getResultCode() != ResultCodeEnum.SUCCESS) {
			throw new LdapSearchException(ldapResult);
		}

		pagedSearchControl = (PagedResults) result.getControl(PagedResults.OID);

		return pagedSearchControl != null && pagedSearchControl.getCookie() != null;
	}

	public Response get() throws CursorException {
		if (cursor == null) {
			throw new CursorException("Search not executed");
		}

		return cursor.get();
	}

	public Entry getEntry() throws CursorException, LdapException {
		if (cursor == null) {
			throw new CursorException("Search not executed");
		}

		return cursor.getEntry();
	}

	@Override
	public Iterator<Response> iterator() {
		return new PagedSearchIterator(this);
	}
}
