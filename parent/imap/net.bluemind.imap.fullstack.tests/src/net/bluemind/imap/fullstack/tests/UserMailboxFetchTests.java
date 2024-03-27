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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.fullstack.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class UserMailboxFetchTests {

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
	}

	@Before
	public void before() throws Exception {
		ElasticsearchTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo, esServer);
		String domUid = "devenv.blue";
		PopulateHelper.addDomain(domUid, Routing.internal);
		String userUid = PopulateHelper.addUser("john", "devenv.blue", Routing.internal);
		assertNotNull(userUid);

		StateContext.setInternalState(new RunningState());
		System.err.println("==== BEFORE ====");

	}

	@After
	public void after() throws Exception {
		System.err.println("===== AFTER =====");
		ElasticsearchTestHelper.getInstance().afterTest();
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testFetchPeekText() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "TEXT", null)) {
					assertNotNull(fetch12);
					System.err.println("Got " + fetch12.size() + " byte(s)");
					assertTrue(fetch12.size() > 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekTextPartialOffsetOutOfRange() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "TEXT", "1084313.2")) {
					assertNotNull(fetch12);
					System.err.println("Got " + fetch12.size() + " byte(s)");
					assertEquals(0, fetch12.size());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekTextPartialLengthTooLong() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "TEXT", "2.454684351")) {
					assertNotNull(fetch12);
					System.err.println("Got " + fetch12.size() + " byte(s)");
					assertTrue(fetch12.size() > 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekEmptySection() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), null, null)) {
					assertNotNull(fetch12);
					assertTrue(fetch12.size() > 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekMime() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "1.MIME", null)) {
					assertNotNull(fetch12);
					System.err.println("Got " + fetch12.size() + " byte(s)");
					assertTrue(fetch12.size() > 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekMimePartial() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "1.MIME", "0.4")) {
					assertNotNull(fetch12);
					System.err.println("Got " + fetch12.size() + " byte(s)");
					assertTrue(fetch12.size() > 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekMimePartialOffsetOutOfRange() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "1.MIME", "45648131.4")) {
					assertNotNull(fetch12);
					System.err.println("Got " + fetch12.size() + " byte(s)");
					assertNotNull(fetch12);
					assertEquals(0, fetch12.size());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekMimePartialLengthTooLong() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "1.MIME", "0.123415646")) {
					assertNotNull(fetch12);
					System.err.println("Got " + fetch12.size() + " byte(s)");
					assertTrue(fetch12.size() > 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekEmptySectionPartial() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), null, "10.20")) {
					assertNotNull(fetch12);
					assertTrue(fetch12.size() > 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekEmptySectionPartialOffsetOutOfRange() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), null, "999999999.2")) {
					assertNotNull(fetch12);
					assertEquals(0, fetch12.size());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekEmptySectionPartialLengthTooLong() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), null, "107840.1646843")) {
					assertNotNull(fetch12);
					assertEquals(0, fetch12.size());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekHeader() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/rfc822_children_in_mail.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "2.HEADER", null)) {
					assertNotNull(fetch12);
					assertTrue(fetch12.size() > 0);
					byte[] data = fetch12.source().read();
					System.err.println("data: " + new String(data));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekHeaderPartialOffsetOutOfRange() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/rfc822_children_in_mail.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "2.HEADER", "124647874.2")) {
					assertNotNull(fetch12);
					assertEquals(0, fetch12.size());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekHeaderLengthTooLong() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/rfc822_children_in_mail.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "2.HEADER", "2.146468")) {
					assertNotNull(fetch12);
					assertTrue(fetch12.size() > 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekHeaderPartial() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/rfc822_children_in_mail.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "2.HEADER", "0.8")) {
					assertNotNull(fetch12);
					assertTrue(fetch12.size() > 0);
					byte[] data = fetch12.source().read();
					System.err.println("data: " + new String(data));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekTextPartial() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "TEXT", "0.4")) {
					assertNotNull(fetch12);
					assertEquals(4, fetch12.size());
					byte[] data = fetch12.source().read();
					System.err.println("data: " + new String(data));
				}
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "TEXT", "0.2048")) {
					assertNotNull(fetch12);
					assertTrue(fetch12.size() > 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchHeaderBadPartial() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "TEXT", "12.8")) {
					assertNotNull(fetch12);
					assertTrue(fetch12.size() > 0);
					byte[] data = fetch12.source().read();
					System.err.println("data: " + new String(data));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchHeaderReturnsNil() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), "1.HEADER", null)) {
					assertNotNull(fetch12);
					assertEquals(0, fetch12.size());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekEmptySectionPartialOffsetOfRange() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), null, "107846.2")) {
					assertNotNull(fetch12);
					assertEquals(0, fetch12.size());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	@Test
	public void testFetchPeekEmptySectionPartialLength() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			try (InputStream in = UserMailboxFetchTests.class.getClassLoader()
					.getResourceAsStream("emls/sapin_inline.eml")) {
				int added = sc.append("INBOX", in, new FlagsList());
				assertTrue(added > 0);
				sc.select("INBOX");
				Collection<Integer> existing = sc.uidSearch(new SearchQuery());
				ArrayList<Integer> newList = new ArrayList<>(existing);
				try (IMAPByteSource fetch12 = sc.uidFetchPart(newList.get(0), null, "2.107846")) {
					assertNotNull(fetch12);
					assertTrue(fetch12.size() > 0);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

}
