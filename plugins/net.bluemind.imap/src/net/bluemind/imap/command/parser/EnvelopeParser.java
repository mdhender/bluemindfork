package net.bluemind.imap.command.parser;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.Address;
import net.bluemind.imap.EncodedWord;
import net.bluemind.imap.Envelope;
import net.bluemind.imap.impl.DateParser;
import net.bluemind.imap.mime.impl.ParenListParser;
import net.bluemind.imap.mime.impl.ParenListParser.TokenType;

public class EnvelopeParser {
	private static final Logger logger = LoggerFactory.getLogger(EnvelopeParser.class);

	/**
	 * <pre>
	 * (        
	 *          "Tue, 19 Jan 2010 09:11:54 +0100" 
	 * 		     "Pb =?ISO-8859-1?Q?r=E9plication_annuaire_ldap?=" 
	 * 		     ( // FROM
	 * 		         ("roberto Malone" NIL "roberto.malone" "cg75.fr")
	 * 		     ) 
	 * 		     ( // SENDER
	 * 		         ("roberto Malone" NIL "roberto.malone" "cg75.fr")
	 * 		     ) 
	 * 		     ( // REPLY TO
	 * 		         ("roberto Malone" NIL "roberto.malone" "cg75.fr")
	 * 		     ) 
	 * 		     ( // TO
	 * 		         (NIL NIL "pikatchooo" "buffle.tlse.lng:support")
	 * 		         (NIL NIL "support" "pikatchooo.fr")
	 * 		     ) 
	 * 		     ( // CC
	 * 		         (NIL NIL "admin.info" "cg75.fr")
	 * 		         ("OLIVIER MALONE" NIL "olivier.molina" "cg75.fr")
	 * 		         ("pikatchooo anthony prades" NIL "anthony.prades" "pikatchooo.fr")
	 * 		     ) 
	 * 		     NIL // BCC
	 * 		     NIL  // IN REPLY TO
	 * 		     "<4B55694A.3000106@cg75.fr>"
	 * 		)
	 * </pre>
	 **/
	public static Envelope parseEnvelope(byte[] env) {
		ParenListParser parser = new ParenListParser();
		int pos = 0;

		pos = parser.consumeToken(pos, env);
		String date = new String(parser.getLastReadToken());
		Date d = null;
		try {
			d = DateParser.parse(date);
		} catch (ParseException e) {
		}

		pos = parser.consumeToken(pos, env);
		String subject = "[Empty subject]";
		if (parser.getLastTokenType() == TokenType.STRING || parser.getLastTokenType() == TokenType.ATOM) {
			subject = EncodedWord.decode(new String(parser.getLastReadToken())).toString();
		}

		// FROM
		pos = parser.consumeToken(pos, env); // (("Raymon" NIL "ra" "cg75.fr"))
		List<Address> from = null;
		if (parser.getLastTokenType() == TokenType.LIST) {
			from = parseList(parser.getLastReadToken(), parser);
		}

		pos = parser.consumeToken(pos, env); // sender
		pos = parser.consumeToken(pos, env); // reply to

		// TO
		pos = parser.consumeToken(pos, env); // (("Raymon" NIL "ra" "cg75.fr"))
		List<Address> to = parseList(parser.getLastReadToken(), parser);

		// CC
		pos = parser.consumeToken(pos, env); // (("Raymon" NIL "ra" "cg75.fr"))
		List<Address> cc = parseList(parser.getLastReadToken(), parser);

		pos = parser.consumeToken(pos, env); // (("Raymon" NIL "ra" "cg75.fr"))
		List<Address> bcc = parseList(parser.getLastReadToken(), parser);

		pos = parser.consumeToken(pos, env); // In-Reply-To
		String inReplyTo = null;
		if (parser.getLastTokenType() != TokenType.NIL) {
			inReplyTo = new String(parser.getLastReadToken());
		}

		pos = parser.consumeToken(pos, env); // Message-ID
		String mid = new String(parser.getLastReadToken());
		Envelope ret = new Envelope(d, subject, to, cc, bcc, extractOne(from), mid, inReplyTo);
		return ret;
	}

	private static Address extractOne(List<Address> from) {
		if (from != null && from.size() > 0) {
			return from.get(0);
		} else {
			try {
				return new Address("admin0@global.virt");
			} catch (Exception e) {
				// unreachable
				return null;
			}
		}
	}

	private static List<Address> parseList(byte[] token, ParenListParser parser) {
		LinkedList<Address> ret = new LinkedList<Address>();

		if (parser.getLastTokenType() != TokenType.LIST) {
			return ret;
		}

		int pos = 0;
		do {
			pos = parser.consumeToken(pos, token);
			byte[] parts = parser.getLastReadToken();
			int p = 0;
			p = parser.consumeToken(p, parts);
			String displayName = null;
			if (parser.getLastTokenType() == TokenType.STRING) {
				displayName = EncodedWord.decode(new String(parser.getLastReadToken())).toString();
			}
			p = parser.consumeToken(p, parts);
			p = parser.consumeToken(p, parts);
			String left = new String(parser.getLastReadToken());
			p = parser.consumeToken(p, parts);
			String right = new String(parser.getLastReadToken());
			Address ad = null;
			try {
				ad = new Address(displayName, left + "@" + right);
				ret.add(ad);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} while (pos < token.length);
		return ret;
	}

}
