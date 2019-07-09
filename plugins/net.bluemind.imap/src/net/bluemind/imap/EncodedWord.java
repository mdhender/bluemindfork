/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Ristretto Mail API.
 *
 * The Initial Developers of the Original Code are
 * Timo Stich and Frederik Dietz.
 * Portions created by the Initial Developers are Copyright (C) 2004
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package net.bluemind.imap;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluemind.imap.impl.QuotedPrintable;

/**
 * Implementation of EncodedWord en- and decoding methods. <br>
 * <b>RFC(s):</b> 2047
 * 
 */
public class EncodedWord {

	/**
	 * QuotedPritntable Encoding. Default.
	 */
	public static final int QUOTED_PRINTABLE = 0;

	/**
	 * Base64 Encoding. Should be used to encode 16bit charsets
	 */
	public static final int BASE64 = 1;

	// finds a encoded word which if of the form
	// =?charset?encoding(b/g)?encoded text part?=
	private static final Pattern encodedWordPattern = Pattern.compile("=\\?([^?]+)\\?([bBqQ])\\?([^?]*)\\?=");

	// filters whitespaces
	private static final Pattern spacePattern = Pattern.compile("\\s*");

	/**
	 * Decodes a string that contains EncodedWords.
	 * 
	 * @param input a string containing EncodedWords
	 * @return the decoded string
	 */
	public static String decode(CharSequence input) {
		StringBuilder result = new StringBuilder(input.length());
		int lastMatchEnd = 0;
		Matcher matcher = encodedWordPattern.matcher(input);
		Charset charset;
		char type;
		String encodedPart;

		while (matcher.find()) {
			CharSequence inbetween = input.subSequence(lastMatchEnd, matcher.start());
			if (!spacePattern.matcher(inbetween).matches()) {
				result.append(inbetween);
			}

			try {
				charset = Charset.forName(matcher.group(1));
			} catch (Exception e) {
				charset = Charset.forName("utf-8");
			}
			type = matcher.group(2).toLowerCase().charAt(0);
			encodedPart = matcher.group(3);

			if (type == 'q') {
				encodedPart = encodedPart.replace('_', ' ');
				// _ are WS and must be converted before normal decoding
				result.append(QuotedPrintable.decode(encodedPart, charset));
			} else {
				result.append(charset.decode(ByteBuffer.wrap(java.util.Base64.getDecoder().decode(encodedPart))));
			}

			lastMatchEnd = matcher.end();
		}

		result.append(input.subSequence(lastMatchEnd, input.length()));

		return result.toString().trim();
	}

	/**
	 * Remove any comments as defined in RFC2822 from the String.
	 * 
	 * @param value
	 * @return the comment-free String
	 */
	public static final String removeComments(String value) {
		final int PLAIN = 0;
		final int QUOTED = 1;
		final int COMMENT = 2;

		StringBuilder result = new StringBuilder(value.length());

		int state = PLAIN;
		int depth = 0;
		char current;

		for (int i = 0; i < value.length(); i++) {
			current = value.charAt(i);

			switch (current) {
			case ('\"'): {
				if (state == COMMENT)
					break;

				if (state == QUOTED)
					state = PLAIN;
				else
					state = QUOTED;
				result.append(current);
				break;
			}

			case ('('): {
				if (state == QUOTED) {
					result.append(current);
					break;
				}

				if (state == COMMENT) {
					depth++;
				} else {
					state = COMMENT;
					depth = 1;
				}
				break;
			}

			case (')'): {
				if (state == QUOTED) {
					result.append(current);
					break;
				}
				if (state == COMMENT) {
					depth--;
					if (depth == 0)
						state = PLAIN;
					break;
				}
				result.append(current);
				break;
			}

			default: {
				if (state != COMMENT)
					result.append(current);
			}

			}
		}

		return result.toString();
	}

}
