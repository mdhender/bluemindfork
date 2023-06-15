package net.bluemind.imap.endpoint.cmd;

import java.nio.charset.StandardCharsets;

import net.bluemind.imap.endpoint.cmd.AnalyzedCommand.FlatCommand;

public class CommandReader {

	private FlatCommand flat;
	private String raw;
	private String rawLow;
	private int index;
	private int max;
	private int litIndex;

	public CommandReader(FlatCommand fc) {
		this.flat = fc;
		this.raw = flat.fullCmd;
		this.rawLow = raw.toLowerCase();
		this.index = 0;
		this.litIndex = 0;
		this.max = raw.length();
	}

	/**
	 * Eats the given command (case insensitive) and position after the space
	 * following the command.
	 * 
	 * @param cmd
	 */
	public void command(String cmd) {
		if (!rawLow.startsWith(cmd.toLowerCase() + " ", index)) {
			throw new ImapReaderException("Command " + raw + " does not start with " + cmd);
		}
		index = cmd.length() + 1;
	}

	/**
	 * Eats a string and position after it
	 * 
	 * @return
	 */
	public String nextString() {
		if (index >= max) {
			throw new ImapReaderException("index " + index + " > command length " + max);
		}

		if (raw.charAt(index) == '"') {
			return quotedString();
		} else if (raw.charAt(index) == '{') {
			return literalString();
		} else {
			return notQuoted();
		}
	}

	private String notQuoted() {
		int nextSpace = raw.indexOf(' ', index);
		if (nextSpace == -1) {
			String ret = raw.substring(index);
			index = max;
			return ret;
		} else {
			String ret = raw.substring(index, nextSpace);
			index = nextSpace;
			return ret;
		}
	}

	private String literalString() {
		int litEnd = raw.indexOf('}', index + 1);
		index = litEnd + 1;
		return flat.literals[litIndex++].toString(StandardCharsets.UTF_8);
	}

	private String quotedString() {
		StringBuilder sb = new StringBuilder();
		boolean escapeNext = false;
		for (int i = index + 1; i < max; i++) {
			char cur = raw.charAt(i);
			index = i + 1;
			if (escapeNext) {
				sb.append(cur);
				escapeNext = false;
			} else if (cur == '\\') {
				escapeNext = true;
			} else if (cur == '"') {
				break;
			} else {
				sb.append(cur);
			}
		}
		return sb.toString();
	}

	/**
	 * Eats a space
	 */
	public void nextSpace() {
		if (raw.charAt(index) == ' ') {
			index++;
		} else {
			throw new ImapReaderException("index " + index + " is not a space");
		}
	}

	@SuppressWarnings("serial")
	public static class ImapReaderException extends RuntimeException {
		public ImapReaderException(String s) {
			super(s);
		}
	}

}
