package net.bluemind.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import com.google.common.base.Strings;

public class HtmlToPlainText {

	public String convert(String html) {
		return getPlainText(documentOf(html));
	}

	public Document documentOf(String html) {
		return Jsoup.parse(html);
	}

	public String getPlainText(Element element) {
		FormattingVisitor formatter = new FormattingVisitor();
		NodeTraversor.traverse(formatter, element);

		return formatter.toString();
	}

	private static class FormattingVisitor implements NodeVisitor {
		private StringBuilder accum = new StringBuilder();

		public void head(Node node, int depth) {
			String name = node.nodeName();
			if (node instanceof TextNode)
				append(((TextNode) node).text());
			else if (name.equals("li"))
				append("\n * ");
			else if (name.equals("dt"))
				append("  ");
			else if (in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr")
					&& (accum.length() != 0 && !in(accum.substring(accum.length() - 1), "\n")))
				append("\n");
		}

		public void tail(Node node, int depth) {
			String name = node.nodeName();
			if (name.equals("a")) {
				String href = node.attributes().get("href");
				if (!Strings.isNullOrEmpty(href)) {
					append(" <" + href + ">");
				}
			} else if (in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5")) {
				append("\n");
			}
		}

		private void append(String text) {
			if (text.equals(" ") && (accum.length() == 0 || in(accum.substring(accum.length() - 1), " ", "\n")))
				return;

			accum.append(text);
		}

		@Override
		public String toString() {
			return accum.toString();
		}

		private boolean in(final String needle, final String... haystack) {
			final int len = haystack.length;
			for (int i = 0; i < len; i++) {
				if (haystack[i].equals(needle))
					return true;
			}
			return false;
		}
	}
}
