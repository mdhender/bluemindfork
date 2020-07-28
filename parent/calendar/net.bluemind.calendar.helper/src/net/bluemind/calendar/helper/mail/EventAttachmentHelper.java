/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.calendar.helper.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.james.mime4j.message.BodyPart;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.icalendar.parser.Mime;

public class EventAttachmentHelper {

	private static final Logger logger = LoggerFactory.getLogger(EventAttachmentHelper.class);

	private static final AsyncHttpClient ahc = createClient(5);

	private static AsyncHttpClient createClient(int timeoutInSeconds) {
		DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
		builder.setUseNativeTransport(Epoll.isAvailable() || KQueue.isAvailable());
		int to = timeoutInSeconds * 1000;
		builder.setConnectTimeout(to).setReadTimeout(to).setRequestTimeout(to).setFollowRedirect(false);
		builder.setTcpNoDelay(true).setThreadPoolName("vevent-attachments-loader").setUseInsecureTrustManager(true);
		builder.setSoReuseAddress(true);
		builder.setMaxRequestRetry(0);
		return new DefaultAsyncHttpClient(builder.build());
	}

	private EventAttachmentHelper() {

	}

	public static boolean hasBinaryAttachments(List<EventAttachment> attachments) {
		return !attachments.isEmpty() && attachments.get(0).isBinaryAttachment();
	}

	public static List<EventAttachment> getAttachments(VEvent event, long maxBytes) {
		long bytesRead = 0;
		List<EventAttachment> attachments = new ArrayList<>();
		try {
			List<EventAttachment> binaryParts = new ArrayList<>(event.attachments.size());
			for (AttachedFile att : event.attachments) {
				bytesRead = fetchIfPossible(maxBytes, bytesRead, binaryParts, att);
			}
			attachments.addAll(binaryParts);
		} catch (FileSizeExceededException fee) {
			logger.warn("vevent '{}' attachments > {} byte(s)", event.summary, maxBytes);
			if (logger.isDebugEnabled()) {
				logger.debug(fee.getMessage(), fee);
			}
			attachments.addAll(event.attachments.stream()
					.map(att -> new EventAttachment(att.publicUrl, att.name, Mime.getMimeType(att.name)))
					.collect(Collectors.toList()));
		}
		return attachments;
	}

	private static long fetchIfPossible(long maxBytes, long bytesRead, List<EventAttachment> binaryParts,
			AttachedFile att) {
		long consumed = bytesRead;
		try {
			byte[] attachmentAsBytes = loadAttachment(att, bytesRead, maxBytes);
			consumed += attachmentAsBytes.length;
			BodyPart binaryPart = new CalendarMailHelper().createBinaryPart(attachmentAsBytes);
			binaryParts.add(new EventAttachment(att.publicUrl, att.name, Mime.getMimeType(att.name), binaryPart));
		} catch (IOException e) {
			logger.warn("Cannot read event attachment from url {}", att.publicUrl, e);
		}
		return consumed;
	}

	private static byte[] loadAttachment(AttachedFile attachment, long read, long maxBytes) throws IOException {
		logger.info("Fetching {}", attachment.publicUrl);

		try {
			BudgetBasedDownloader dl = new BudgetBasedDownloader(maxBytes - read);
			Optional<byte[]> response = ahc.prepareGet(attachment.publicUrl).execute(dl).get(10, TimeUnit.SECONDS);
			return response.orElseThrow(FileSizeExceededException::new);
		} catch (FileSizeExceededException fe) {
			throw fe;
		} catch (Exception e1) {
			throw new IOException("Error loading vevent attachment @ " + attachment.publicUrl, e1);
		}
	}

	@SuppressWarnings("serial")
	public static class FileSizeExceededException extends RuntimeException {
	}

}
