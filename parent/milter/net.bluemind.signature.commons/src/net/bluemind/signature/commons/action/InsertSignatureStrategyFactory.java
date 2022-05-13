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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

package net.bluemind.signature.commons.action;

import org.jsoup.nodes.Element;

public class InsertSignatureStrategyFactory {

	public static InsertSignatureStrategy create(String content, String usePlaceholder) {
		if (signatureCanBePlaced(usePlaceholder)) {
			int start = content.indexOf(AddDisclaimer.PLACEHOLDER_PREFIX);
			int end;
			if (start != -1 && (end = content.indexOf(AddDisclaimer.PLACEHOLDER_SUFFIX,
					start + AddDisclaimer.PLACEHOLDER_PREFIX.length())) != -1) {
				return new InsertInsidePlaceholders(start, end);
			} else if (content.indexOf(AddDisclaimer.LEGACY_PLACEHOLDER) != -1) {
				return new ReplaceLegacyPlaceholder();
			}
		}
		return new InsertAtEnd();
	}

	private static boolean signatureCanBePlaced(String usePlaceholder) {
		return Boolean.TRUE.equals(Boolean.valueOf(usePlaceholder));
	}

	interface InsertSignatureStrategy {
		String insertSignature(String content, String signature);

		void insertSignature(Element body, String signature);
	}

	private static class InsertInsidePlaceholders implements InsertSignatureStrategy {

		private final int start;
		private final int end;

		public InsertInsidePlaceholders(int start, int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public String insertSignature(String content, String signature) {
			StringBuilder str = new StringBuilder(content);
			str.replace(start, end + AddDisclaimer.PLACEHOLDER_SUFFIX.length(), signature);
			return str.toString();
		}

		@Override
		public void insertSignature(Element body, String signature) {
			StringBuilder str = new StringBuilder(body.html());
			str.replace(start, end + AddDisclaimer.PLACEHOLDER_SUFFIX.length(), signature);
			body.html(str.toString());
		}

	}

	private static class ReplaceLegacyPlaceholder implements InsertSignatureStrategy {

		@Override
		public String insertSignature(String content, String signature) {
			return content.replaceFirst(AddDisclaimer.LEGACY_PLACEHOLDER, signature);
		}

		@Override
		public void insertSignature(Element body, String signature) {
			Element placeholderElement = body.getElementsContainingOwnText(AddDisclaimer.LEGACY_PLACEHOLDER).get(0);
			String html = placeholderElement.html().replaceFirst(AddDisclaimer.LEGACY_PLACEHOLDER, signature);
			placeholderElement.html(html);
		}

	}

	private static class InsertAtEnd implements InsertSignatureStrategy {

		@Override
		public String insertSignature(String content, String signature) {
			return content + signature;
		}

		@Override
		public void insertSignature(Element body, String signature) {
			body.append(signature);
		}

	}

}
