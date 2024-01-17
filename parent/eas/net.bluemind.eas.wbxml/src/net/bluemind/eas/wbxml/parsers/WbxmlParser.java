/* BEGIN LICENSE
 * See readme.html
 * END LICENSE
 */
package net.bluemind.eas.wbxml.parsers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A SAX-based parser for WBXML.
 * 
 */
public final class WbxmlParser {

	private static final Logger logger = LoggerFactory.getLogger(WbxmlParser.class);

	private InputStream in;
	private final ContentHandler dh;
	private final WbxmlExtensionHandler eh;
	private final Map<Integer, NamespacedTable> tagsTables;

	private NamespacedTable tagTable;
	private char[] stringTable;

	private int publicIdentifierId;
	private Deque<String> stack;

	private String docCharset;

	/**
	 * WBXML used by ActiveSync has no attributes
	 */
	private static final Attributes NO_ATTRIBUTES_IN_EAS = new AttributesImpl();

	public WbxmlParser(ContentHandler dh, WbxmlExtensionHandler eh) {
		this.stack = new ArrayDeque<>();
		this.tagsTables = ParserTagsTablesIndex.get();
		this.dh = dh;
		this.eh = eh;
		switchPage(0);
	}

	/**
	 * Sets the tag table for a given page. The first string in the array
	 * defines tag 5, the second tag 6 etc. Currently, only page 0 is supported
	 */
	public void setTagTable(int page, NamespacedTable table) {
		tagsTables.put(page, table);
	}

	public void parse(InputStream in) throws SAXException, IOException {

		char entityBuf[] = new char[1];

		this.in = in;

		readByte(); // skip version
		publicIdentifierId = readInt();

		if (publicIdentifierId == 0) {
			readInt();
		}

		int charset = readInt();
		docCharset = new CharsetMappings().getCharset(charset);
		logger.debug("document charset is {}", docCharset);

		int strTabSize = readInt();
		stringTable = new char[strTabSize];

		for (int i = 0; i < strTabSize; i++) {
			stringTable[i] = (char) readByte();
		}

		// ok, now the real thing....

		dh.startDocument();

		while (true) {
			int id = in.read();
			if (id == -1) {
				break;
			}

			switch (id) {
			case Wbxml.SWITCH_PAGE:
				int page = readByte();
				switchPage(page);
				break;

			case Wbxml.END:
				String elem = stack.pop();
				dh.endElement(null, null, elem);
				break;

			case Wbxml.ENTITY:
				entityBuf[0] = (char) readInt();
				dh.characters(entityBuf, 0, 1);
				break;

			case Wbxml.STR_I: {
				String s = readStrI();
				dh.characters(s.toCharArray(), 0, s.length());
				break;
			}

			case Wbxml.EXT_I_0:
			case Wbxml.EXT_I_1:
			case Wbxml.EXT_I_2:
			case Wbxml.EXT_T_0:
			case Wbxml.EXT_T_1:
			case Wbxml.EXT_T_2:
			case Wbxml.EXT_0:
			case Wbxml.EXT_1:
			case Wbxml.EXT_2:
			case Wbxml.OPAQUE:
				handleExtensions(id);
				break;

			case Wbxml.PI:
				throw new SAXException("PI curr. not supp.");
				// readPI;
				// break;

			case Wbxml.STR_T: {
				int pos = readInt();
				int end = pos;
				while (stringTable[end] != 0) {
					end++;
				}
				dh.characters(stringTable, pos, end - pos);
				break;
			}

			default:
				readElement(id);
			}
		}
		if (!stack.isEmpty()) {
			throw new SAXException("unclosed elements: " + stack);
		}

		dh.endDocument();
	}

	public void switchPage(int page) {
		tagTable = tagsTables.get(page);
		logger.debug("switching to page 0x" + page);
		if (tagTable == null) {
			logger.debug("tagsTable not found for page " + page);
		}
	}

	// -------------- internal methods start here --------------------

	void handleExtensions(int id) throws SAXException, IOException {
		switch (id) {
		case Wbxml.EXT_I_0:
		case Wbxml.EXT_I_1:
		case Wbxml.EXT_I_2:
			eh.ext_i(id - Wbxml.EXT_I_0, readStrI());
			break;

		case Wbxml.EXT_T_0:
		case Wbxml.EXT_T_1:
		case Wbxml.EXT_T_2:
			eh.ext_t(id - Wbxml.EXT_T_0, readInt());
			break;

		case Wbxml.EXT_0:
		case Wbxml.EXT_1:
		case Wbxml.EXT_2:
			eh.ext(id - Wbxml.EXT_0);
			break;

		case Wbxml.OPAQUE: {
			int len = readInt();
			byte[] buf = new byte[len];
			for (int i = 0; i < len; i++) {
				buf[i] = (byte) readByte();
			}

			eh.opaque(buf);
		} // case OPAQUE
		} // SWITCH
	}

	private String resolveId(String[] tab, int id) throws SAXException, IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("resolve(0x" + Integer.toHexString(id & 0x07f) + ")");
		}
		int idx = (id & 0x07f) - 5;
		if (idx == -1) {
			return readStrT();
		}
		if (idx < 0 || tab == null || idx >= tab.length || tab[idx] == null) {
			throw new SAXException("id " + id + " idx " + idx + " undef. tab: " + tab);
		}

		String ret = tab[idx];
		if (logger.isDebugEnabled()) {
			logger.debug("resolved as '" + ret + "'");
		}

		return ret;
	}

	void readElement(int id) throws IOException, SAXException {
		String tag = resolveId(tagTable.tagsTable, id & 0x03f);

		dh.startElement(tagTable.namespace, tag, tag, NO_ATTRIBUTES_IN_EAS);

		if ((id & 64) != 0) {
			stack.push(tag);
		} else {
			dh.endElement(tagTable.namespace, tag, tag);
		}
	}

	int readByte() throws IOException, SAXException {
		int i = in.read();
		if (i == -1) {
			throw new SAXException("Unexpected EOF");
		}
		return i;
	}

	int readInt() throws SAXException, IOException {
		int result = 0;
		int i;

		do {
			i = readByte();
			result = (result << 7) | (i & 0x7f);
		} while ((i & 0x80) != 0);

		return result;
	}

	String readStrI() throws IOException, SAXException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		while (true) {
			int i = in.read();
			if (i == -1) {
				throw new SAXException("Unexpected EOF");
			}
			if (i == 0) {
				return new String(out.toByteArray(), docCharset);
			}
			out.write(i);
		}
	}

	String readStrT() throws IOException, SAXException {
		int pos = readInt();
		int end = pos;

		while (stringTable[end] != 0) {
			end++;
		}

		return new String(stringTable, pos, end - pos);
	}
}
