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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.parsetools.JsonEventType;
import io.vertx.core.parsetools.JsonParser;
import net.bluemind.config.InstallationId;

public class ParseKafkaJsonDumpTests {

	@Test
	public void parseJsonObjects() throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/kafka/clone-dump.tar.bz2");
				BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(in);
				TarArchiveInputStream tar = new TarArchiveInputStream(bz2)) {
			TarArchiveEntry ce;
			String iid = "bluemind-" + UUID.randomUUID().toString();
			int iidLen = iid.length();

			while ((ce = tar.getNextTarEntry()) != null) {
				if (!ce.isDirectory()) {
					iid = ce.getName().replace("./", "").substring(0, iidLen);
					System.setProperty("bm.mcast.id", iid);
					InstallationId.reload();

					byte[] jsonData = ByteStreams.toByteArray(tar);
					System.err.println(jsonData.length + " byte(s) of json file");
					JsonArray expected = new JsonArray(Buffer.buffer(jsonData));
					JsonParser parser = JsonParser.newParser().objectValueMode();
					LongAdder objectcount = new LongAdder();
					parser.handler(js -> {
						System.err.println("js: " + js.type());
						if (js.type() == JsonEventType.VALUE) {
							objectcount.increment();
						}
					});
					parser.write(Buffer.buffer(jsonData)).end();
					assertEquals(expected.size(), objectcount.sum());
				}
			}
		}
	}

}
