package net.bluemind.imap.sieve;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;

public class SieveMessageParser {

	private final static byte[] delimBuf = new byte[] { (byte) '\r', (byte) '\n' };

	private enum State {
		Normal, Literral, LiterralLength;
	}

	private StringBuffer currentLine = new StringBuffer();
	private int literralLength;
	private StringBuffer literralLengthValue;
	private State currentState = State.Normal;
	private int matchCount = 0;
	private List<String> lines = new ArrayList<>();
	private SieveMessage message;

	public boolean parse(IoBuffer in) {

		while (in.hasRemaining()) {
			byte c = in.get();

			switch (currentState) {
			case Literral:
				currentLine.append((char) c);
				literralLength--;
				if (literralLength == 0) {
					currentState = State.Normal;
				}
				break;
			case LiterralLength:
				if (c == '}') {
					currentState = State.Normal;
					literralLength = Integer.parseInt(literralLengthValue.toString());
					literralLengthValue = null;
				} else {
					literralLengthValue.append((char) c);
				}
				break;
			case Normal:
				if (c == '{') {
					// begin literral length read
					literralLengthValue = new StringBuffer();
					currentState = State.LiterralLength;
				} else if (delimBuf[matchCount] == c) {
					matchCount++;
					if (matchCount == delimBuf.length) {
						matchCount = 0;
						if (literralLength > 0) {
							currentState = State.Literral;
						} else {
							// end of line
							String line = currentLine.toString();
							if (line.startsWith("OK") || line.startsWith("NO") || line.startsWith("BYE")) {
								message = new SieveMessage();
								message.setResponseMessage(line);
								message.getLines().addAll(lines);
								return true;
							} else {
								lines.add(line);
								currentLine = new StringBuffer();
							}
						}

					}
				} else {
					currentLine.append((char) c);
				}
				break;
			default:
				break;
			}
		}
		return false;
	}

	public SieveMessage getMessage() {
		return message;
	}

	public void reset() {

		currentLine = new StringBuffer();
		literralLength = 0;
		literralLengthValue = null;
		currentState = State.Normal;
		matchCount = 0;
		lines = new ArrayList<>();
		message = null;

	}

}