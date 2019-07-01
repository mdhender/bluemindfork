/* BEGIN LICENSE
 * See readme.html
 * END LICENSE
 */
package net.bluemind.eas.wbxml.writers;

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.common.base.Throwables;

import net.bluemind.eas.wbxml.TagsTables;
import net.bluemind.eas.wbxml.WbxmlOutput;
import net.bluemind.eas.wbxml.parsers.Wbxml;

/**
 * Converts XML to WBXML using a sax content handler
 */
public class WbxmlEncoder {

	private static final byte[] wbxmlHeader = new byte[] { 0x03, // version
			0x01, // unknown or missing public identifier
			0x6a, // UTF-8
			0x00 // no string table
	};

	private static final Logger logger = LoggerFactory.getLogger(WbxmlEncoder.class);
	private Map<String, Integer> stringTable;
	private final WbxmlOutput buf;
	private final String defaultNamespace;

	/**
	 * The constructor creates an internal document handler. The given parser is
	 * used
	 */
	public WbxmlEncoder(String defaultNamespace, WbxmlOutput buf) {
		this.defaultNamespace = defaultNamespace;
		this.buf = buf;
	}

	void setStringTable(Map<String, Integer> table) {
		this.stringTable = table;
	}

	/**
	 * converts the XML data from the given SAX InputSource and writes the
	 * result to the given OutputStream
	 */
	public void convert(Source in) throws SAXException, IOException {

		header();

		// write buf with transformed content
		EncoderHandler handler = new EncoderHandler(this, defaultNamespace);
		SAXResult result = new SAXResult(handler);
		try {
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.transform(in, result);
		} catch (Exception e) {
			throw new IOException(e);
		}

		// ready!
		buf.end();
	}

	// internal methods

	private void writeInt(int i) throws IOException {
		byte[] tmp = new byte[5];
		int idx = 0;

		do {
			tmp[idx++] = (byte) (i & 0x7f);
			i = i >> 7;
		} while (i != 0);

		while (idx > 1) {
			buf.write(tmp[--idx] | 0x80);
		}
		buf.write(tmp[0]);
	}

	public void writeStrI(String s) throws IOException {
		buf.write(Wbxml.STR_I);
		buf.write(s.getBytes());
		buf.write(0);
	}

	public void startString() throws IOException {
		buf.write(Wbxml.STR_I);
	}

	public void endString() throws IOException {
		buf.write(0);
	}

	@SuppressWarnings("unused")
	private void writeStrT(String s) throws IOException {

		Integer idx = stringTable.get(s);

		if (idx == null) {
			throw new IOException("unknown elem in mapping table: " + s);
		}

		writeInt(idx.intValue());
	}

	private void switchPage(Integer integer) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("*********************************** switching to page 0x" + Integer.toHexString(integer));
		}
		writeInt(0x00);
		writeInt(integer);
	}

	public void switchNamespace(String ns) throws IOException {
		Map<String, Integer> table = TagsTables.getElementMappings(ns);
		setStringTable(table);
		switchPage(TagsTables.NAMESPACES_IDS.get(ns));
	}

	public void writeElement(String name) throws IOException {
		Integer mapping = stringTable.get(name);
		if (mapping == null) {
			logger.warn("no mapping for '" + name + "'");
			StringBuilder tableContent = new StringBuilder();
			for (String s : stringTable.keySet()) {
				tableContent.append(' ').append(s);
			}
			logger.warn("tableContent is{}", tableContent.toString());
			throw new IOException("no mapping for '" + name + "'");
		}
		writeInt(stringTable.get(name) + 64);
	}

	public void writeEmptyElement(String name) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("write empty tag " + name);
		}
		Integer mapping = stringTable.get(name);
		if (mapping == null) {
			logger.warn("no mapping for '" + name + "'");
			throw new IOException("no mapping for '" + name + "'");
		}
		writeInt(stringTable.get(name));
	}

	public void end() {
		try {
			buf.write(Wbxml.END);
		} catch (IOException e) {
			Throwables.propagate(e);
		}
	}

	public void startByteArray(int len) throws IOException {
		buf.write(Wbxml.OPAQUE);
		writeInt(len);
	}

	public void endByteArray() {
		// do nothing
	}

	public void writeByteArray(byte[] bytes) throws IOException {
		buf.write(Wbxml.OPAQUE);
		writeInt(bytes.length);
		buf.write(bytes);
	}

	public void header() throws IOException {
		buf.write(wbxmlHeader);
	}
}
