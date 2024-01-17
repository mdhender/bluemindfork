package net.bluemind.eas.data.formatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.junit.Test;

import net.bluemind.utils.HtmlToPlainText;

public class PlainBodyFormatterTest {

	@Test
	public void testJsoupWith1397043() throws Exception {
		Path path = Paths.get("data/1397043.html");
		double memory = convert(path, //
				read -> new HtmlToPlainText().documentOf(read), //
				doc -> new HtmlToPlainText().getPlainText(doc));
		assertTrue(memory < 60);
	}

	@Test
	public void testJsoupWith486328() throws Exception {
		Path path = Paths.get("data/486328.html");
		double memory = convert(path, //
				html -> new HtmlToPlainText().documentOf(html), //
				doc -> new HtmlToPlainText().getPlainText(doc));
		assertTrue(memory < 40);

	}

	@Test
	public void testContentWithoutHtmlAndBodyTag() throws Exception {
		String html = "<div class=WordSection1><p class=MsoNormal>One<o:p></o:p></p></div>";
		String text = new HtmlToPlainText().convert(html);
		assertEquals("One\n", text);
	}

	@Test
	public void testHref() {
		String html = "<a href=\"www.bluemind.net\">THIS IS BLUEMIND</a>";
		String text = new HtmlToPlainText().convert(html);
		assertEquals("THIS IS BLUEMIND <www.bluemind.net>", text);

		html = "<a nothref=\"www.bluemind.net\">THIS IS BLUEMIND</a>";
		text = new HtmlToPlainText().convert(html);
		assertEquals("THIS IS BLUEMIND", text);

	}

	private <T> double convert(Path path, Function<String, T> map, Function<T, String> convert) throws Exception {
		String read = Files.readAllLines(path).stream().collect(Collectors.joining("\n"));
		System.gc();
		Runtime runtime = Runtime.getRuntime();
		long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
		T doc = map.apply(read);
		System.gc();
		long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
		double delta = (memoryAfter - memoryBefore) / 1000000.0;

		String output = convert.apply(doc);
		System.out.println(output);
		return delta;
	}

	private interface Function<T, U> {
		U apply(T t) throws Exception;
	}

}
