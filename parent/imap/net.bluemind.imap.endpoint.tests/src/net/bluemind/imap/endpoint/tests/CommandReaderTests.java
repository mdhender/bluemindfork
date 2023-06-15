package net.bluemind.imap.endpoint.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import net.bluemind.imap.endpoint.cmd.AnalyzedCommand.FlatCommand;
import net.bluemind.imap.endpoint.cmd.CommandReader;

public class CommandReaderTests {

	@Test
	public void testParseUnquotedString() {
		FlatCommand fc = FlatCommand.of("select abcd");
		CommandReader cr = new CommandReader(fc);
		cr.command("Select");
		String fold = cr.nextString();
		assertEquals("abcd", fold);
	}

	@Test
	public void testParseQuotedStringWithFollowup() {
		FlatCommand fc = FlatCommand.of("select \"abcd\" (qresync)");
		CommandReader cr = new CommandReader(fc);
		cr.command("seLect");
		String fold = cr.nextString();
		assertEquals("abcd", fold);
	}

	@Test
	public void testParseQuotedStringAtEnd() {
		FlatCommand fc = FlatCommand.of("select \"abcd\"");
		CommandReader cr = new CommandReader(fc);
		cr.command("Select");
		String fold = cr.nextString();
		assertEquals("abcd", fold);
	}

	@Test
	public void testParseQuotedStringWithEscape() {
		FlatCommand fc = FlatCommand.of("select \"abcd\\\"\"");
		CommandReader cr = new CommandReader(fc);
		cr.command("Select");
		String fold = cr.nextString();
		assertEquals("abcd\"", fold);
	}

	@Test
	public void testParseEmpty() {
		FlatCommand fc = FlatCommand.of("select \"\"");
		CommandReader cr = new CommandReader(fc);
		cr.command("Select");
		String fold = cr.nextString();
		assertEquals("", fold);
	}

	@Test
	public void testLoginStyle() {
		FlatCommand fc = FlatCommand.of("login tom \"pou\\\"ce\"");
		CommandReader cr = new CommandReader(fc);
		cr.command("LOGIN");
		String log = cr.nextString();
		cr.nextSpace();
		String pass = cr.nextString();
		assertEquals("tom", log);
		assertEquals("pou\"ce", pass);
	}

	@Test
	public void testLoginStyleAtomic() {
		FlatCommand fc = FlatCommand.of("login tom {ATOM_0}", Unpooled.wrappedBuffer("pou\"ce".getBytes()));
		CommandReader cr = new CommandReader(fc);
		cr.command("LOGIN");
		String log = cr.nextString();
		cr.nextSpace();
		String pass = cr.nextString();
		assertEquals("tom", log);
		assertEquals("pou\"ce", pass);
	}

}
