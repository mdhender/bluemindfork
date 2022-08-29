package net.bluemind.system.importation.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.system.importation.search.PagedSearchResult.LdapSearchException;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper.DeleteTreeException;

public class PagedSearchResultTests {
	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	@Test
	public void next() throws LdapInvalidDnException, LdapException, DeleteTreeException, IOException, CursorException,
			LdapSearchException {
		LdapDockerTestHelper.initLdapTree(this.getClass(), "/resources/search/pagedSearchResult.ldif");

		SearchRequest searchRequest = new SearchRequestImpl().setBase(new Dn("dc=local")).setScope(SearchScope.SUBTREE)
				.setFilter("(uid=*)").addAttributes("*").setSizeLimit(0);
		PagedSearchResult pagedSearchResult = new PagedSearchResult(LdapDockerTestHelper.getLdapCon(), searchRequest,
				2);

		Set<String> found = new HashSet<>();
		while (pagedSearchResult.next()) {
			Response response = pagedSearchResult.get();
			if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
				System.out.println(String.format("Entry ignored - type: %s", response.getType()));
				continue;
			}

			String entryDn = ((SearchResultEntry) response).getEntry().getDn().getName();

			assertEquals(entryDn, pagedSearchResult.getEntry().getDn().getName());

			System.out.println(String.format("Found %s", entryDn));

			assertFalse(found.contains(entryDn));
			found.add(entryDn);
		}

		assertEquals(5, found.size());
	}

	@Test
	public void iterrator() throws LdapInvalidDnException, LdapException, DeleteTreeException, IOException,
			CursorException, LdapSearchException {
		LdapDockerTestHelper.initLdapTree(this.getClass(), "/resources/search/pagedSearchResult.ldif");

		SearchRequest searchRequest = new SearchRequestImpl().setBase(new Dn("dc=local")).setScope(SearchScope.SUBTREE)
				.setFilter("(uid=*)").addAttributes("*").setSizeLimit(0);
		PagedSearchResult pagedSearchResult = new PagedSearchResult(LdapDockerTestHelper.getLdapCon(), searchRequest,
				2);

		Set<String> found = new HashSet<>();
		for (Response response : pagedSearchResult) {
			if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
				System.out.println(String.format("Entry ignored - type: %s", response.getType()));
				continue;
			}

			String entryDn = ((SearchResultEntry) response).getEntry().getDn().getName();

			System.out.println(String.format("Found %s", entryDn));

			assertFalse(found.contains(entryDn));
			found.add(entryDn);
		}

		assertEquals(5, found.size());
	}

	@Test
	public void spliterrator() throws LdapInvalidDnException, LdapException, DeleteTreeException, IOException,
			CursorException, LdapSearchException {
		LdapDockerTestHelper.initLdapTree(this.getClass(), "/resources/search/pagedSearchResult.ldif");

		SearchRequest searchRequest = new SearchRequestImpl().setBase(new Dn("dc=local")).setScope(SearchScope.SUBTREE)
				.setFilter("(uid=*)").addAttributes("*").setSizeLimit(0);
		PagedSearchResult pagedSearchResult = new PagedSearchResult(LdapDockerTestHelper.getLdapCon(), searchRequest,
				2);

		Set<String> found = StreamSupport.stream(pagedSearchResult.spliterator(), false)
				.filter(response -> response.getType() == MessageTypeEnum.SEARCH_RESULT_ENTRY)
				.map(response -> ((SearchResultEntry) response).getEntry().getDn().getName())
				.collect(Collectors.toSet());

		assertEquals(5, found.size());
	}
}
