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
package net.bluemind.xmpp.server;

import static tigase.server.Message.ELEM_NAME;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import tigase.db.DBInitException;
import tigase.db.MsgRepositoryIfc;
import tigase.db.NonAuthUserRepository;
import tigase.db.UserNotFoundException;
import tigase.server.Packet;
import tigase.util.DNSResolver;
import tigase.util.TigaseStringprepException;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPPostprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.Presence;

/**
 * OfflineMessages plugin implementation which follows
 * <a href="http://xmpp.org/extensions/xep-0160.html">XEP-0160: Best Practices
 * for Handling Offline Messages</a> specification. Responsible for storing
 * messages send to offline users - either as a standalone plugin or as a
 * processor for other plugins (e.g. AMP). Is registered to handle packets of
 * type {@code <presence>}.
 * 
 * 
 * Created: Mon Oct 16 13:28:53 2006
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class BMOfflineMessages extends XMPPProcessor implements XMPPPostprocessorIfc, XMPPProcessorIfc {

	private static final String OFFLINE_PATH = "/usr/share/bm-xmpp/offline/";

	/**
	 * Field holds default client namespace for stanzas. In case of
	 * {@code msgoffline} plugin it is <em>jabber:client</em>
	 */
	protected static final String XMLNS = "jabber:client";
	/**
	 * Field holds identification string for the plugin. In case of
	 * {@code msgoffline} plugin it is <em>msgoffline</em>
	 */
	private static final String ID = "bm-offline-messages";
	/** Private logger for class instances. */
	private static final Logger logger = LoggerFactory.getLogger(BMOfflineMessages.class);

	/**
	 * Field holds an array for element paths for which the plugin offers
	 * processing capabilities. In case of {@code msgoffline} plugin it is
	 * <em>presence</em> stanza
	 */
	private static final String[][] ELEMENTS = { { Presence.PRESENCE_ELEMENT_NAME } };
	/**
	 * Field holds an array of name-spaces for stanzas which can be processed by
	 * this plugin. In case of {@code msgoffline} plugin it is
	 * <em>jabber:client</em>
	 */
	private static final String[] XMLNSS = { XMLNS };
	/**
	 * Field holds an array of XML Elements with service discovery features
	 * which have to be returned to the client uppon request. In case of
	 * {@code msgoffline} plugin it is the same as plugin name -
	 * <em>msgoffline</em>
	 */
	private static final Element[] DISCO_FEATURES = {
			new Element("feature", new String[] { "var" }, new String[] { "msgoffline" }) };
	/** Field holds the default hostname of the machine. */
	private static final String defHost = DNSResolver.getDefaultHostname();
	/**
	 * Field holds an array for element paths for which the plugin offers
	 * message saving capabilities. In case of {@code msgoffline} plugin it is
	 * <em>presence</em> stanza
	 */
	public static final String[] MESSAGE_EVENT_PATH = { ELEM_NAME, "event" };
	/**
	 * Field holds an array for element paths for which the plugin offers
	 * processing capabilities. In case of {@code msgoffline} plugin it is
	 * <em>presence</em> stanza
	 */
	public static final String[] MESSAGE_HEADER_PATH = { ELEM_NAME, "header" };
	// ~--- fields
	// ---------------------------------------------------------------
	/**
	 * Field holds class for formatting and parsing dates in a locale-sensitive
	 * manner
	 */
	private final SimpleDateFormat formatter;

	{
		this.formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		this.formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public BMOfflineMessages() {
		logger.info("**************** bm-offline-messages loaded ***********");
		File f = new File(OFFLINE_PATH);
		f.mkdirs();
	}

	// ~--- methods
	// --------------------------------------------------------------
	@Override
	public int concurrentQueuesNo() {
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public String id() {
		return ID;
	}

	/**
	 * OfflineMessages postprocessor simply calls
	 * {@code savePacketForOffLineUser} method to store packet to offline
	 * repository.
	 * <p>
	 * 
	 * {@inheritDoc}
	 * 
	 * @param conn
	 * @param queue
	 */
	@Override
	public void postProcess(final Packet packet, final XMPPResourceConnection conn, final NonAuthUserRepository repo,
			final Queue<Packet> queue, Map<String, Object> settings) {

		boolean hasSession = false;
		try {
			hasSession = BMUserRepo.getSessionManager().hasSession(packet.getStanzaTo().getBareJID().toString());
		} catch (TigaseStringprepException e1) {
			logger.error(e1.getMessage(), e1);
		}

		if (!hasSession) {
			try {
				MsgRepositoryIfc msg_repo = getMsgRepoImpl(repo, conn);

				boolean saved = savePacketForOffLineUser(packet, msg_repo);

				if (!saved) {
					// XEP-0160
					// offline queue is full, sends <service-unavailable/> to
					// sender.
					logger.info("{}'s offline queue is full. send <service-unavailable/> to {}",
							packet.getStanzaTo().toString(), packet.getStanzaFrom().toString());

					try {
						Packet error = Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
								"Service not available.", true);
						error.setPacketTo(packet.getStanzaFrom());

						queue.add(error);

					} catch (PacketErrorTypeException e) {
						logger.error(e.getMessage(), e);
					}

				}

			} catch (UserNotFoundException e) {
				logger.error("UserNotFoundException at trying to save packet for off-line user." + packet);
			} // end of try-catch
		} // end of if (hasSession == null)

	}

	/**
	 * {@code OfflineMessages} processor is triggered by {@code <presence>}
	 * stanza. Upon receiving it plugin tries to load messages from repository
	 * and, if the result is not empty, sends them to the user
	 * 
	 * {@inheritDoc}
	 * 
	 * 
	 * @param conn
	 * @throws NotAuthorizedException
	 */
	@Override
	public void process(final Packet packet, final XMPPResourceConnection conn, final NonAuthUserRepository repo,
			final Queue<Packet> results, final Map<String, Object> settings) throws NotAuthorizedException {
		if (loadOfflineMessages(packet, conn)) {
			try {
				MsgRepositoryIfc msg_repo = getMsgRepoImpl(repo, conn);
				Queue<Packet> packets = restorePacketForOffLineUser(conn, msg_repo);
				if (packets != null) {
					logger.debug("Sending off-line messages: " + packets.size());
					results.addAll(packets);
				}
			} catch (UserNotFoundException e) {
				logger.error("Something wrong, DB problem, cannot load offline messages. " + e);
			} // end of try-catch
		}
	}

	/**
	 * Method restores all messages from repository for the JID of the current
	 * session. All retrieved elements are then instantiated as {@code Packet}
	 * objects added to {@code LinkedList} collection and, if possible, sorted
	 * by timestamp.
	 * 
	 * @param conn
	 *            user session which keeps all the user session data and also
	 *            gives an access to the user's repository data.
	 * @param repo
	 *            an implementation of {@link MsgRepositoryIfc} interface
	 * 
	 * 
	 * @return a {@link Queue} of {@link Packet} objects based on all stored
	 *         payloads for the JID of the current session.
	 * 
	 * @throws UserNotFoundException
	 * @throws NotAuthorizedException
	 */
	public Queue<Packet> restorePacketForOffLineUser(XMPPResourceConnection conn, MsgRepositoryIfc repo)
			throws UserNotFoundException, NotAuthorizedException {
		Queue<Element> elems = repo.loadMessagesToJID(conn, true);

		if (elems != null) {
			logger.info("restore packet for {}", conn.getjid());
			if (logger.isDebugEnabled()) {
				logger.debug(" * packets: {}", elems);
			}
			LinkedList<Packet> pacs = new LinkedList<Packet>();
			Element elem = null;

			while ((elem = elems.poll()) != null) {
				try {
					pacs.offer(Packet.packetInstance(elem));
				} catch (TigaseStringprepException ex) {
					logger.error("Packet addressing problem, stringprep failed: " + elem);
				}
			} // end of while (elem = elems.poll() != null)
			try {
				Collections.sort(pacs, new StampComparator());
			} catch (NullPointerException e) {
				try {
					logger.error("Can not sort off line messages: " + pacs + ",\n" + e);
				} catch (Exception exc) {
					logger.error("Can not print log message.");
				}
			}

			return pacs;
		}

		return null;
	}

	/**
	 * Method stores messages to offline repository with the following rules
	 * applied, i.e. saves only:
	 * <li>message stanza with either nonempty {@code <body>}, {@code <event>}
	 * or {@code <header>} child element and only messages of type normal, chat.
	 * <li>presence stanza of type subscribe, subscribed, unsubscribe and
	 * unsubscribed.
	 * <p>
	 * Processed messages are stamped with the {@code delay} element and
	 * appropriate timestamp.
	 * <p>
	 * 
	 * 
	 * @param pac
	 *            a {@link Packet} object containing packet that should be
	 *            verified and saved
	 * @param repo
	 *            a {@link MsgRepositoryIfc} repository handler responsible for
	 *            storing messages
	 * 
	 * @return {@code true} if the packet was correctly saved to repository,
	 *         {@code false} otherwise.
	 * 
	 * @throws UserNotFoundException
	 */
	public boolean savePacketForOffLineUser(Packet pac, MsgRepositoryIfc repo) throws UserNotFoundException {
		StanzaType type = pac.getType();

		// save only:
		// presence stanza of type subscribe, subscribed, unsubscribe and
		// unsubscribed
		if ((pac.getElemName().equals("message")
				&& (pac.getElemCDataStaticStr(tigase.server.Message.MESSAGE_BODY_PATH) != null)
				&& ((type == null) || (type == StanzaType.normal) || (type == StanzaType.chat)))
				|| (pac.getElemName().equals("presence")
						&& ((type == StanzaType.subscribe) || (type == StanzaType.subscribed)
								|| (type == StanzaType.unsubscribe) || (type == StanzaType.unsubscribed)))) {

			StringBuilder path = new StringBuilder();
			path.append(OFFLINE_PATH);
			path.append(pac.getStanzaTo().getBareJID().toString());
			path.append(".bin");
			File f = new File(path.toString());
			if (f.exists() && f.length() > (100 * 1000)) { // 100ko
				// XEP-0160
				// offline queue is full, sends <service-unavailable/> to
				// sender.
				return false;
			}

			Element elem = pac.getElement().clone();
			String stamp = null;

			synchronized (formatter) {
				stamp = formatter.format(new Date());
			}

			String from = pac.getStanzaTo().getDomain();
			Element x = new Element("delay", "Offline Storage - " + defHost, new String[] { "from", "stamp", "xmlns" },
					new String[] { from, stamp, "urn:xmpp:delay" });

			elem.addChild(x);
			repo.storeMessage(pac.getStanzaFrom(), pac.getStanzaTo(), null, elem, null);
			pac.processedBy(ID);

		} else {
			logger.debug("Packet for offline user not suitable for storing: " + pac);
		}

		return true;
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	// ~--- get methods
	// ----------------------------------------------------------
	/**
	 * Method allows obtaining instance of {@link MsgRepositoryIfc} interface
	 * implementation.
	 * 
	 * @param conn
	 *            user session which keeps all the user session data and also
	 *            gives an access to the user's repository data.
	 * @param repo
	 *            an implementation of {@link MsgRepositoryIfc} interface
	 * 
	 * @return instance of {@link MsgRepositoryIfc} interface implementation.
	 */
	protected MsgRepositoryIfc getMsgRepoImpl(NonAuthUserRepository repo, XMPPResourceConnection conn) {
		return new MsgRepositoryImpl(repo, conn);
	}

	// ~--- methods
	// --------------------------------------------------------------
	/**
	 * Method determines whether offline messages should be loaded - the process
	 * should be run only once per user session and only for available/null
	 * presence with priority greater than 0.
	 * 
	 * 
	 * @param packet
	 *            a {@link Packet} object containing packet that should be
	 *            verified and saved
	 * @param conn
	 *            user session which keeps all the user session data and also
	 *            gives an access to the user's repository data.
	 * 
	 * @return {@code true} if the messages should be loaded, {@code false}
	 *         otherwise.
	 */
	protected boolean loadOfflineMessages(Packet packet, XMPPResourceConnection conn) {

		// If the user session is null or the user is anonymous just
		// ignore it.
		if ((conn == null) || conn.isAnonymous()) {
			logger.debug("do not load offline messages: conn == null?" + (conn == null) + ", conn.isAnonymous()?"
					+ conn.isAnonymous());
			return false;
		} // end of if (session == null)

		// Try to restore the offline messages only once for the user session
		if (conn.getSessionData(ID) != null) {
			logger.debug("do not load offline messages: conn.getSessionData(ID) != null?" + ID);
			return false;
		}

		StanzaType type = packet.getType();

		if ((type == null) || (type == StanzaType.available)) {
			// Should we send off-line messages now?
			// Let's try to do it here and maybe later I find better place.
			String priority_str = packet.getElemCDataStaticStr(tigase.server.Presence.PRESENCE_PRIORITY_PATH);
			int priority = 0;

			if (priority_str != null) {
				try {
					priority = Integer.decode(priority_str);
				} catch (NumberFormatException e) {
					priority = 0;
				} // end of try-catch
			} // end of if (priority != null)

			if (priority >= 0) {
				conn.putSessionData(ID, ID);
				return true;
			} // end of if (priority >= 0)
		} // end of if (type == null || type == StanzaType.available)

		return false;
	}

	// ~--- inner classes
	// --------------------------------------------------------
	/**
	 * Implementation of {@code MsgRepositoryIfc} interface providing basic
	 * support for storing and loading of Elements from repository.
	 */
	private class MsgRepositoryImpl implements MsgRepositoryIfc {

		/**
		 * Field holds a reference to user session which keeps all the user
		 * session data and also gives an access to the user's repository data.
		 */
		private SimpleParser parser = SingletonFactory.getParserInstance();

		// ~--- constructors
		// -------------------------------------------------------
		/**
		 * Constructs {@code MsgRepositoryImpl} object referencing user session
		 * and having handle to user repository.
		 * 
		 * @param repo
		 *            an implementation of {@link MsgRepositoryIfc} interface
		 * @param conn
		 *            user session which keeps all the user session data and
		 *            also gives an access to the user's repository data.
		 */
		private MsgRepositoryImpl(NonAuthUserRepository repo, XMPPResourceConnection conn) {
		}

		// ~--- get methods
		// --------------------------------------------------------
		@Override
		public Element getMessageExpired(long time, boolean delete) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		// ~--- methods
		// ------------------------------------------------------------
		@Override
		public Queue<Element> loadMessagesToJID(XMPPResourceConnection con, boolean delete)
				throws UserNotFoundException {
			JID to;
			try {
				to = con.getJID();
			} catch (NotAuthorizedException e1) {
				throw new RuntimeException(e1);
			}
			DomBuilderHandler domHandler = new DomBuilderHandler();

			StringBuilder path = new StringBuilder();
			path.append(OFFLINE_PATH);
			path.append(to.getBareJID().toString());
			path.append(".bin");
			File f = new File(path.toString());

			logger.info(" * Restore file {}", path.toString());

			if (f.exists()) {
				try {
					StringBuilder sb = new StringBuilder();
					sb.append(new String(Files.toByteArray(f)));

					logger.info(" * Delete offline file {}", path);
					if (!f.delete()) {
						logger.error("Fail to remove file {}", path);
					}

					if (sb.length() > 0) {
						char[] content = sb.toString().toCharArray();
						parser.parse(domHandler, content, 0, content.length);

						return domHandler.getParsedElements();
					} // end of while (elem = elems.poll() != null)
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}

			return null;
		}

		@Override
		public boolean storeMessage(JID from, JID to, Date expired, Element msg, NonAuthUserRepository noneAuthUserRep)
				throws UserNotFoundException {

			StringBuilder path = new StringBuilder();
			path.append(OFFLINE_PATH);
			path.append(to.getBareJID().toString());
			path.append(".bin");
			File f = new File(path.toString());

			try {
				Files.append(msg.toString(), f, Charsets.UTF_8);
				logger.info("store message for {}, in {}, size {}", to.toString(), path, f.length());
				return true;
			} catch (IOException e) {
				logger.error("fail to store message " + msg.toString() + "\n");
				return false;
			}

		}

		@Override
		public void initRepository(String arg0, Map<String, String> arg1) throws DBInitException {
			// TODO Auto-generated method stub

		}

	}

	/**
	 * {@link Comparator} interface implementation for the purpose of sorting
	 * Elements retrieved from the repository by the timestamp stored in
	 * {@code delay} element.
	 */
	private class StampComparator implements Comparator<Packet> {

		@Override
		public int compare(Packet p1, Packet p2) {
			String stamp1 = "";
			String stamp2 = "";

			// Try XEP-0203 - the new XEP...
			Element stamp_el1 = p1.getElement().getChild("delay", "urn:xmpp:delay");

			if (stamp_el1 == null) {

				// XEP-0091 support - the old one...
				stamp_el1 = p1.getElement().getChild("x", "jabber:x:delay");
			}
			stamp1 = stamp_el1.getAttributeStaticStr("stamp");

			// Try XEP-0203 - the new XEP...
			Element stamp_el2 = p2.getElement().getChild("delay", "urn:xmpp:delay");

			if (stamp_el2 == null) {

				// XEP-0091 support - the old one...
				stamp_el2 = p2.getElement().getChild("x", "jabber:x:delay");
			}
			stamp2 = stamp_el2.getAttributeStaticStr("stamp");

			return stamp1.compareTo(stamp2);
		}
	}
}
