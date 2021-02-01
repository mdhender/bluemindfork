/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.linkify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.Leniency;

import android.util.Patterns;

public class Linkify {

	private static final ExecutorService executor = Executors.newCachedThreadPool();

	/**
	 * MatchFilter enables client code to have more control over what is allowed to
	 * match and become a link, and what is not.
	 *
	 * For example: when matching web URLs you would like things like
	 * http://www.example.com to match, as well as just example.com itelf. However,
	 * you would not want to match against the domain in support@example.com. So,
	 * when matching against a web URL pattern you might also include a MatchFilter
	 * that disallows the match if it is immediately preceded by an at-sign (@).
	 */
	public interface MatchFilter {
		/**
		 * Examines the character span matched by the pattern and determines if the
		 * match should be turned into an actionable link.
		 *
		 * @param s     The body of text against which the pattern was matched
		 * @param start The index of the first character in s that was matched by the
		 *              pattern - inclusive
		 * @param end   The index of the last character in s that was matched -
		 *              exclusive
		 *
		 * @return Whether this match should be turned into a link
		 */
		boolean acceptMatch(CharSequence s, int start, int end);
	}

	/**
	 * TransformFilter enables client code to have more control over how matched
	 * patterns are represented as URLs.
	 *
	 * For example: when converting a phone number such as (919) 555-1212 into a
	 * tel: URL the parentheses, white space, and hyphen need to be removed to
	 * produce tel:9195551212.
	 */
	public interface TransformFilter {
		/**
		 * Examines the matched text and either passes it through or uses the data in
		 * the Matcher state to produce a replacement.
		 *
		 * @param match The regex matcher state that found this URL text
		 * @param url   The text that was matched
		 *
		 * @return The transformed form of the URL
		 */
		String transformUrl(final Matcher match, String url);
	}

	private enum LinkKind {
		url(0),

		email(7), // 'mailto:' length

		tel(4); // 'tel:' length

		private int formatSubstring;

		private LinkKind(int f) {
			this.formatSubstring = f;
		}
	}

	private static class LinkSpec {
		public LinkSpec(LinkKind kind, String url, int start, int end) {
			this.kind = kind;
			this.url = url;
			this.start = start;
			this.end = end;
		}

		final LinkKind kind;
		final String url;
		final int start;
		final int end;

		@Override
		public String toString() {
			return MoreObjects.toStringHelper("LinkSpec")//
					.add("kind", kind)//
					.add("url", url)//
					.add("range", "[" + start + "-" + end + "]").toString();
		}

		public String format() {
			return "<a href=\"" + url + "\">" + url.substring(kind.formatSubstring) + "</a>";
		}

	}

	/**
	 * Filters out web URL matches that occur after an at-sign (@). This is to
	 * prevent turning the domain name in an email address into a web link.
	 */
	public static final MatchFilter sUrlMatchFilter = (CharSequence s, int start, int end) -> {
		if (start == 0) {
			return true;
		}

		return (s.charAt(start - 1) == '@') ? false : true;
	};

	private Linkify() {
	}

	private static final Pattern inDiamonds = Pattern.compile("<([^>]*)>");

	/**
	 * Performs simple CRLF replacements and link detection
	 * 
	 * @param s
	 * @return
	 */
	public static String toHtml(String s) {
		Future<String> future = executor.submit(() -> toHtmlImpl(s));
		try {
			return future.get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			future.cancel(true);
			return s;
		}
	}

	private static String toHtmlImpl(String s) {
		if (s.contains("</div>") || s.contains("</table>")) {
			return s;
		}

		CharMatcher lfMatcher = CharMatcher.is('\n');
		String trans = CharMatcher.is('\r').removeFrom(s);

		Matcher matcher = inDiamonds.matcher(trans);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(sb, " $1 ");
		}
		matcher.appendTail(sb);
		trans = sb.toString();

		trans = lfMatcher.collapseFrom(trans, '\n');
		trans = lfMatcher.replaceFrom(trans, "<br>\n");

		List<LinkSpec> links = new ArrayList<>();
		gatherLinks(links, LinkKind.url, trans, Patterns.AUTOLINK_WEB_URL,
				new String[] { "http://", "https://", "rtsp://", "ftp://" }, sUrlMatchFilter);

		gatherLinks(links, LinkKind.email, trans, Patterns.AUTOLINK_EMAIL_ADDRESS, new String[] { "mailto:" }, null);

		gatherTelLinks(links, trans);

		pruneOverlaps(links);
		if (links.isEmpty()) {
			return trans;
		}

		StringBuilder result = new StringBuilder();
		int start = 0;
		for (LinkSpec ls : links) {
			String prefix = trans.substring(start, ls.start);
			result.append(prefix);
			result.append(ls.format());
			start = ls.end;
		}
		result.append(trans.substring(start));

		return result.toString();
	}

	private static final void gatherLinks(List<LinkSpec> links, LinkKind k, CharSequence s, Pattern pattern,
			String[] schemes, MatchFilter matchFilter) {
		Matcher m = pattern.matcher(s);

		while (m.find()) {
			int start = m.start();
			int end = m.end();

			if (matchFilter == null || matchFilter.acceptMatch(s, start, end)) {
				String url = makeUrl(m.group(0), schemes);
				LinkSpec spec = new LinkSpec(k, url, start, end);

				links.add(spec);
			}
		}
	}

	private static void gatherTelLinks(List<LinkSpec> links, String s) {
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		Iterable<PhoneNumberMatch> matches = phoneUtil.findNumbers(s.toString(), "FR", Leniency.POSSIBLE,
				Long.MAX_VALUE);
		for (PhoneNumberMatch match : matches) {
			String url = "tel:" + PhoneUtils.normalizeNumber(match.rawString());
			LinkSpec spec = new LinkSpec(LinkKind.tel, url, match.start(), match.end());
			links.add(spec);
		}
	}

	private static final String makeUrl(String url, String[] prefixes) {

		boolean hasPrefix = false;

		for (int i = 0; i < prefixes.length; i++) {
			if (url.regionMatches(true, 0, prefixes[i], 0, prefixes[i].length())) {
				hasPrefix = true;

				// Fix capitalization if necessary
				if (!url.regionMatches(false, 0, prefixes[i], 0, prefixes[i].length())) {
					url = prefixes[i] + url.substring(prefixes[i].length());
				}

				break;
			}
		}

		if (!hasPrefix && prefixes.length > 0) {
			url = prefixes[0] + url;
		}

		return url;
	}

	private static final Comparator<LinkSpec> comp = (LinkSpec a, LinkSpec b) -> {
		if (a.start < b.start) {
			return -1;
		}

		if (a.start > b.start) {
			return 1;
		}

		if (a.end < b.end) {
			return 1;
		}

		if (a.end > b.end) {
			return -1;
		}

		return 0;
	};

	private static final void pruneOverlaps(List<LinkSpec> links) {

		Collections.sort(links, comp);

		int len = links.size();
		int i = 0;

		while (i < len - 1) {
			LinkSpec a = links.get(i);
			LinkSpec b = links.get(i + 1);
			int remove = -1;

			if ((a.start <= b.start) && (a.end > b.start)) {
				if (b.end <= a.end) {
					remove = i + 1;
				} else if ((a.end - a.start) > (b.end - b.start)) {
					remove = i + 1;
				} else if ((a.end - a.start) < (b.end - b.start)) {
					remove = i;
				}

				if (remove != -1) {
					links.remove(remove);
					len--;
					continue;
				}

			}

			i++;
		}
	}

}
