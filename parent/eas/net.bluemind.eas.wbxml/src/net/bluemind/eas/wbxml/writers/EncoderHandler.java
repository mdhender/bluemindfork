/* BEGIN LICENSE
 * See readme.html
 * END LICENSE
 */
package net.bluemind.eas.wbxml.writers;

import java.io.IOException;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.bluemind.eas.wbxml.parsers.DataTypeUtil;

class EncoderHandler extends DefaultHandler {
	protected Logger logger = LoggerFactory.getLogger(getClass());
	private WbxmlEncoder we;
	private String currentXmlns;
	private StringBuilder currentCharacter;
	private String currentQName;

	private Stack<String> stackedStarts;

	public EncoderHandler(WbxmlEncoder we, String defaultNamespace) throws IOException {
		this.stackedStarts = new Stack<String>();
		this.we = we;
		try {
			switchToNs(defaultNamespace);
		} catch (SAXException e) {
			logger.error(e.getMessage(), e);
		}
		currentXmlns = defaultNamespace;
	}

	public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {

		if (!stackedStarts.isEmpty()) {
			flushNormal();
		}
		flushCharacter();
		try {
			logger.debug("uri: {}, local: {}, qName: {}", uri, localName, qName);
			if (!uri.equals(currentXmlns)) {
				switchToNs(uri);
			}
			currentXmlns = uri;
			currentQName = currentXmlns + ":" + localName;

			// we.writeElement(qName);
			queueStart(localName);
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	private void queueStart(String qName) {
		stackedStarts.add(qName);
	}

	private void flushNormal() throws SAXException {
		String e = stackedStarts.pop();
		try {
			we.writeElement(e);
		} catch (IOException e1) {
			logger.error(e1.getMessage(), e1);
			throw new SAXException(e);
		}
	}

	private void flushEmptyElem() throws SAXException {
		String e = stackedStarts.pop();
		try {
			we.writeEmptyElement(e);
		} catch (IOException e1) {
			logger.error(e1.getMessage(), e1);
			throw new SAXException(e);
		}
	}

	private void switchToNs(String newNs) throws IOException, SAXException {
		if (!stackedStarts.isEmpty()) {
			flushNormal();
		}
		we.switchNamespace(newNs);
	}

	public void characters(char[] chars, int start, int len) throws SAXException {
		if (!stackedStarts.isEmpty()) {
			flushNormal();
		}
		String s = new String(chars, start, len);
		appendCharacter(s);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (!stackedStarts.isEmpty()) {
			flushEmptyElem();
		} else {
			flushCharacter();
			we.end();
		}
	}

	private void appendCharacter(String characters) {

		if (this.currentCharacter == null) {
			if (characters.trim().length() == 0) {
				return;
			}
			this.currentCharacter = new StringBuilder();
		}
		currentCharacter.append(characters);
	}

	private void flushCharacter() {
		if (this.currentCharacter != null) {
			try {
				if (DataTypeUtil.isByteArray(currentQName)) {
					we.writeByteArray(java.util.Base64.getDecoder().decode(currentCharacter.toString()));
				} else {
					we.writeStrI(currentCharacter.toString());
				}
				currentCharacter = null;
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		if (!stackedStarts.isEmpty()) {
			flushNormal();
		}
	}

}
