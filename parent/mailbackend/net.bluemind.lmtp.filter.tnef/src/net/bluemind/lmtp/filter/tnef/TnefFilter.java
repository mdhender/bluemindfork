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
package net.bluemind.lmtp.filter.tnef;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Realm.AuthScheme;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;
import com.google.common.io.ByteStreams;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.lmtp.backend.FilterException;
import net.bluemind.lmtp.backend.IMessageFilter;
import net.bluemind.lmtp.backend.LmtpEnvelope;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.network.topology.Topology;

public class TnefFilter implements IMessageFilter {

	private static final Logger logger = LoggerFactory.getLogger(TnefFilter.class);

	private final Supplier<ClientSideServiceProvider> apiProv;

	private DefaultAsyncHttpClient ahc;

	public TnefFilter() {
		apiProv = Suppliers.memoize(() -> prov(Token.admin0()));
		this.ahc = new DefaultAsyncHttpClient();
	}

	private ClientSideServiceProvider prov(String token) {
		String core = Topology.get().core().value.address();
		String addr = "http://" + core + ":8090";
		return ClientSideServiceProvider.getProvider(addr, token).setOrigin("lmtp-tnef");
	}

	@Override
	public Message filter(LmtpEnvelope env, Message message, long messageSize) throws FilterException {
		return MapiEndpoint.any().map(endpoint -> withEndpoint(endpoint, env, message)).orElse(null);
	}

	private Message withEndpoint(String endpoint, LmtpEnvelope env, Message message) {
		if (message.isMultipart()) {
			Multipart mp = (Multipart) message.getBody();
			List<AddressableEntity> tree = Mime4JHelper.expandTree(mp.getBodyParts());
			Optional<AddressableEntity> winmailDat = tree.stream()
					.filter(ae -> "application/ms-tnef".equalsIgnoreCase(ae.getMimeType())
							|| "winmail.dat".equalsIgnoreCase(ae.getFilename()))
					.findAny();
			return winmailDat.map(tnef -> {
				Message fromTnef = rewriteWinmail(endpoint, env.getRecipients().get(0).getEmailAddress(),
						(SingleBody) tnef.getBody());
				if (fromTnef != null) {
					// ensure the message-id from the original message is maintained
					fromTnef.getHeader().setField(message.getHeader().getField("Message-ID"));
					fromTnef.setTo(message.getTo());
					fromTnef.setCc(message.getCc());
					fromTnef.setFrom(message.getFrom());
					logger.info("MAPI endpoint has rewritten '{}' from TNEF.", fromTnef.getSubject());
				}
				return fromTnef;
			}).orElse(null);
		}
		return null;
	}

	private Message rewriteWinmail(String endpoint, String email, SingleBody tnef) {

		logger.info("Use {} to process tnef sent to {}, body is {}", endpoint, email, tnef.getClass());
		IAuthentication authApi = apiProv.get().instance(IAuthentication.class);
		LoginResponse sudo = authApi.su(email);
		if (sudo.status != Status.Ok) {
			throw new ServerFault("Failed to sudo as " + email);
		}
		Realm realm = new Realm.Builder(sudo.latd, sudo.authKey).setScheme(AuthScheme.BASIC)
				.setCharset(StandardCharsets.UTF_8).build();
		BoundRequestBuilder post = ahc.preparePost(endpoint).setRealm(realm);
		try (InputStream in = tnef.getInputStream()) {
			byte[] read = ByteStreams.toByteArray(in);
			logger.info("Read {} byte(s) of tnef data", read.length);
			post.setBody(read);
			Response response = post.execute().get(20, TimeUnit.SECONDS);
			byte[] freshEml = response.getResponseBodyAsBytes();
			if (freshEml.length == 0) {
				Path copy = Files.createTempFile("winmail-lmtpd-unparsed", ".dat");
				Files.write(copy, read, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.WRITE);
				logger.error(
						"MAPI endpoint failed to analyse tnef, a copy was saved to {}. Mail will be delivered as-is.",
						copy.toFile().getAbsolutePath());
				return null;
			} else {
				logger.info("Re-generated EML size is {} byte(s)", freshEml.length);
				return Mime4JHelper.parse(new ByteArrayInputStream(freshEml));
			}
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			prov(sudo.authKey).instance(IAuthentication.class).logout();
		}
		return null;
	}

}
