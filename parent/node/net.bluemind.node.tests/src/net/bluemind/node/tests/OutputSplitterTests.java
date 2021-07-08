/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.node.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.Handler;
import net.bluemind.node.server.busmod.OutputSplitter;
import net.bluemind.node.server.busmod.OutputSplitter.Line;

public class OutputSplitterTests {

	@Test
	public void test2AsciiLinesOneShot() {

		JoinHandler jh = new JoinHandler();
		OutputSplitter os = new OutputSplitter(jh);
		os.write(Unpooled.wrappedBuffer("ab\ncd\n".getBytes()));
		os.end();
		assertArrayEquals(new String[] { "ab", "cd" }, jh.output());
		assertEquals(2, jh.count());

	}

	@Test
	public void testLFFrist() {
		String[] input = new String[] { "\nlafille\n" };

		JoinHandler jh = new JoinHandler();
		OutputSplitter os = new OutputSplitter(jh);
		for (String in : input) {
			ByteBuf buf = Unpooled.wrappedBuffer(in.getBytes(StandardCharsets.UTF_8));
			os.write(buf);
		}
		os.end();
		assertArrayEquals(new String[] { "", "lafille" }, jh.output());
	}

	@Test
	public void testChunking() {
		String[] input = new String[] { "\n", "lafi", "lle\ndu" };

		JoinHandler jh = new JoinHandler();
		OutputSplitter os = new OutputSplitter(jh);
		for (String in : input) {
			ByteBuf buf = Unpooled.wrappedBuffer(in.getBytes(StandardCharsets.UTF_8));
			os.write(buf);
		}
		os.end();
		assertArrayEquals(new String[] { "", "lafille", "du" }, jh.output());
	}

	@Test
	public void testUnicodeChunking() {
		// we have 2 bytes, 3 bytes & 4 bytes utf-8 characters in there
		String inputStr = "€urôëôëôëôë☞cééééé€é€éé€éé€ééé𓃴éééééééééééééèr$";
		byte[] asBytes = inputStr.getBytes();

		for (int splitPoint = 1; splitPoint < asBytes.length - 1; splitPoint++) {
			ByteBuf full = Unpooled.wrappedBuffer(asBytes);

			ByteBuf firstHalf = full.readSlice(splitPoint).copy();
			ByteBuf secHalf = full.copy();
			int frameSize = 5;
			JoinHandler jh = new JoinHandler(frameSize);
			OutputSplitter os = new OutputSplitter(jh, frameSize);
			os.write(firstHalf).write(secHalf).end();
			jh.end();
			System.err.println("Out: " + Arrays.toString(jh.output()));
			assertArrayEquals("Failed with splitpoint " + splitPoint,
					new String[] { "€urôëôëôëôë☞cééééé€é€éé€éé€ééé𓃴éééééééééééééèr$" }, jh.output());
			System.out.println("Success with splitpoint " + splitPoint);
		}

	}

	@Test
	public void testSimpleUnicode() {
		String[] input = new String[] { "la\nfille\nd", "u\nbé", "douin" };

		JoinHandler jh = new JoinHandler();
		OutputSplitter os = new OutputSplitter(jh);
		for (String in : input) {
			ByteBuf buf = Unpooled.wrappedBuffer(in.getBytes(StandardCharsets.UTF_8));
			os.write(buf);
		}
		os.end();
		assertArrayEquals(new String[] { "la", "fille", "du", "bédouin" }, jh.output());
	}

	public static class JoinHandler implements Handler<Line> {
		List<String> saved;
		StringBuilder cur;
		private int maxFrame;

		public JoinHandler() {
			this(OutputSplitter.DEFAULT_MAX_FRAME_SIZE);
		}

		public JoinHandler(int maxFrame) {
			saved = new ArrayList<>();
			cur = new StringBuilder();
			this.maxFrame = maxFrame;
		}

		public int count() {
			return saved.size();
		}

		public void end() {
			if (cur.length() > 0) {
				saved.add(cur.toString());
			}
		}

		public String[] output() {
			return saved.toArray(new String[saved.size()]);
		}

		@Override
		public void handle(Line event) {
			assertFalse("LF in output", event.log.contains("\n"));
			System.err.println("H l: '" + event.log + "', continued: " + event.continued);
			byte[] logBytes = event.log.getBytes();
			assertTrue("maxFrameSize " + maxFrame + " was not honored, got " + logBytes.length,
					logBytes.length <= maxFrame);
			cur.append(event.log);
			if (!event.continued) {
				saved.add(cur.toString());
				cur.setLength(0);
			}
		}

	}

}
