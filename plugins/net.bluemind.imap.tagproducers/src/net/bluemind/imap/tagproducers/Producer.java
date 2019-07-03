/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.imap.tagproducers;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.ITagProducer;

/**
 * {@link ITagProducer} factory for imitating the behavior of our supported IMAP
 * clients.
 */
public class Producer {

	private static final char[] choices = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
			'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9' };
	private static final Logger logger = LoggerFactory.getLogger(Producer.class);

	public static ITagProducer outlookStyle() {
		return new ITagProducer() {
			final Random r = new Random();
			String last = gen();

			private String gen() {
				StringBuilder sb = new StringBuilder();
				sb.append(choices[r.nextInt(choices.length)]);
				sb.append(choices[r.nextInt(choices.length)]);
				sb.append(choices[r.nextInt(choices.length)]);
				sb.append(choices[r.nextInt(choices.length)]);
				String r = sb.toString();
				logger.info("Generated {}", r);
				return r;
			}

			@Override
			public String nextTag() {
				last = gen();
				return last;
			}

			@Override
			public String currentTag() {
				return last;
			}
		};
	}

	public static ITagProducer tbirdStyle() {
		return new ITagProducer() {

			long count = 0;

			@Override
			public String nextTag() {
				return "" + (++count);
			}

			@Override
			public String currentTag() {
				return "" + count;
			}
		};
	}

	public static ITagProducer appleMailStyle() {
		return new ITagProducer() {

			long count = 0;

			@Override
			public String nextTag() {
				return "1." + (++count);
			}

			@Override
			public String currentTag() {
				return "1." + count;
			}
		};
	}
}
