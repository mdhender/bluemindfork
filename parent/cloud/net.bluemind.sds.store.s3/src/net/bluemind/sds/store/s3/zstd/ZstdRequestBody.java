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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.sds.store.s3.zstd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.luben.zstd.ZstdOutputStream;
import com.google.common.io.ByteStreams;
import com.netflix.spectator.api.DistributionSummary;

import software.amazon.awssdk.core.async.AsyncRequestBody;

public class ZstdRequestBody implements AsyncRequestBody {

	private static final Logger logger = LoggerFactory.getLogger(ZstdRequestBody.class);
	private IOException savedError;
	private RandomAccessFile raf;
	private MappedByteBuffer mmap;
	private long len;

	public ZstdRequestBody(Path sourceFile, DistributionSummary compressionRatio) {
		Path tmpPath = null;
		try (InputStream in = Files.newInputStream(sourceFile)) {
			tmpPath = Files.createTempFile("eml", ".zst");
			try (OutputStream out = Files.newOutputStream(tmpPath, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
					ZstdOutputStream zos = new ZstdOutputStream(out)) {
				ByteStreams.copy(in, zos);
			}
			this.raf = new RandomAccessFile(tmpPath.toFile(), "r");
			long origLen = Files.size(sourceFile);
			this.len = Files.size(tmpPath);
			long compressionPercent = len * 100 / origLen;
			compressionRatio.record(Math.max(0, 100 - compressionPercent));
			this.mmap = raf.getChannel().map(MapMode.READ_ONLY, 0, len);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			this.savedError = e;
		} finally {
			if (tmpPath != null) {
				try {
					Files.deleteIfExists(tmpPath);
				} catch (IOException e) {
					// OK
				}
			}
		}
	}

	@Override
	public void subscribe(Subscriber<? super ByteBuffer> s) {
		Subscription sub = new Subscription() {

			private boolean cancelled;

			@Override
			public void request(long n) {
				if (savedError != null) {
					s.onError(savedError);
					return;
				}
				for (int i = 0; i < n && !cancelled; i++) {
					int remain = mmap.remaining();
					int toGrab = Math.min(32768, remain);
					if (toGrab == 0) {
						try {
							raf.close();
						} catch (IOException e) {
							// ok
						}
						s.onComplete();
						break;
					} else {
						byte[] tgt = new byte[toGrab];
						mmap.get(tgt);
						s.onNext(ByteBuffer.wrap(tgt));
					}
				}

			}

			@Override
			public void cancel() {
				this.cancelled = true;
			}

		};
		s.onSubscribe(sub);

	}

	@Override
	public Optional<Long> contentLength() {
		return Optional.of(len);
	}

}
