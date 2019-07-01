package net.bluemind.calendar.pdf.internal;

import org.w3c.dom.Element;

public class NodeUtils {

	public static void setText(Element elt, String text) {
		elt.appendChild(elt.getOwnerDocument().createTextNode(text));
	}

	public static String geText(Element elt) {
		if( elt.getFirstChild() == null) {
			return "";
		}
		return elt.getFirstChild().getNodeValue();
	}

}
