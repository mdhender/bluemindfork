package net.bluemind.mailbox.api.utils;

import java.text.Normalizer;

public class StripAccents {

	private StripAccents() {

	}

	public static String strip(String value) {
		String s = Normalizer.normalize(value, Normalizer.Form.NFD);
		s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
		return retainAscii(s);
	}

	private static String retainAscii(String s) {
		// CharMatcher.ascii().retainFrom(s) would pull Guava in API bundle...
		char upperLimit = '\u007f';
		StringBuilder sb = new StringBuilder(s.length());
		s.chars().filter(asInt -> asInt <= upperLimit).forEach(asInt -> sb.append((char) asInt));
		return sb.toString();
	}

}
