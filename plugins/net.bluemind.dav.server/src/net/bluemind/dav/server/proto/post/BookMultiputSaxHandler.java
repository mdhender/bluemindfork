package net.bluemind.dav.server.proto.post;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.bluemind.addressbook.adapter.VCardAdapter;
import net.bluemind.dav.server.proto.NS;
import net.fortuna.ical4j.vcard.VCard;

public class BookMultiputSaxHandler extends DefaultHandler {

	private static final Logger logger = LoggerFactory.getLogger(BookMultiputSaxHandler.class);

	private final List<VCardPut> vcards;
	private final StringBuilder sb;
	private boolean inVcard;
	private boolean inHref;
	private String updateHref;
	private boolean isDelete;

	public BookMultiputSaxHandler() {
		vcards = new LinkedList<>();
		sb = new StringBuilder(512);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if ("href".equals(localName) && NS.WEBDAV.equals(uri)) {
			inHref = true;
		} else if ("address-data".equals(localName) && NS.CARDDAV.equals(uri)) {
			inVcard = true;
			sb.setLength(0);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if ("href".equals(localName) && NS.WEBDAV.equals(uri)) {
			inHref = false;
			updateHref = sb.toString();
			sb.setLength(0);
		} else if ("address-data".equals(localName) && NS.CARDDAV.equals(uri)) {
			inVcard = false;
			String vcf = sb.toString();
			try {
				List<VCard> cards = VCardAdapter.parse(vcf);
				if (cards.size() > 1) {
					logger.error("Multiple cards ({}) submitted in one vcf, don't know how to handle that....",
							cards.size());
					int i = 0;
					for (VCard vc : cards) {
						logger.info("CARD {}: '{}'", ++i, vc);
					}
				}
				vcards.add(new VCardPut(cards.get(0), updateHref));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			updateHref = null;
		} else if ("delete".equals(localName) && NS.ME_COM.equals(uri)) {
			isDelete = true;
		} else if ("resource".equals(localName) && NS.ME_COM.equals(uri)) {
			if (isDelete) {
				vcards.add(new VCardPut(null, updateHref));
				isDelete = false;
			}
			sb.setLength(0);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (inVcard || inHref) {
			sb.append(ch, start, length);
		}
	}

	public List<VCardPut> getVcards() {
		return vcards;
	}
}