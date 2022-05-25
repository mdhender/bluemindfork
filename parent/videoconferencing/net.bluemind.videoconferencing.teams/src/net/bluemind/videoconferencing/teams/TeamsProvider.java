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
package net.bluemind.videoconferencing.teams;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.LobbyBypassScope;
import com.microsoft.graph.models.LobbyBypassSettings;
import com.microsoft.graph.models.OnlineMeeting;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.OnlineMeetingCollectionRequest;
import com.microsoft.graph.requests.OnlineMeetingRequest;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirEntryPath;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.user.api.IInternalUserExternalAccount;
import net.bluemind.user.api.UserAccount;
import net.bluemind.videoconferencing.api.IVideoConferencingProvider;
import net.bluemind.videoconferencing.api.VideoConference;
import okhttp3.Request;

public class TeamsProvider implements IVideoConferencingProvider {

	/**
	 * Teams joinInformation content: data:text/html,ENCODED_HTML_CONTENT
	 */
	private static Pattern DATA_URI = Pattern.compile("(?s)data:([^,]*?),(.*)$");

	public static final String PROVIDER_NAME = "Teams";

	@Override
	public String id() {
		return "videoconferencing-teams";
	}

	@Override
	public String name() {
		return PROVIDER_NAME;
	}

	@Override
	public Optional<byte[]> getIcon() {
		try {
			return Optional.of(ByteStreams
					.toByteArray(TeamsProvider.class.getClassLoader().getResourceAsStream("resources/logo.png")));
		} catch (IOException e) {
		}
		return Optional.empty();
	}

	@Override
	public VideoConference getConferenceInfo(BmContext context, Map<String, String> resourceSettings,
			ItemValue<ResourceDescriptor> resource, VEvent vevent) throws ServerFault {

		String clientId = resourceSettings.get("clientId");
		String secret = resourceSettings.get("secret");
		String tenant = resourceSettings.get("tenant");

		Optional<String> organizer = resolveOrganizer(context, vevent);
		if (!organizer.isPresent()) {
			throw new ServerFault("Failed to fetch organizer " + vevent.organizer);
		}

		IInternalUserExternalAccount externalAccountService = ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).instance(IInternalUserExternalAccount.class,
						context.getSecurityContext().getContainerUid(), organizer.get());

		UserAccount externalAccount = externalAccountService.get(PROVIDER_NAME);
		if (externalAccount == null) {
			throw ServerFault.notFound("No external account found for organizer: " + organizer.get());
		}

		ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder().clientId(clientId)
				.clientSecret(secret).tenantId(tenant).build();
		TokenCredentialAuthProvider tokenCredAuthProvider = new TokenCredentialAuthProvider(clientSecretCredential);
		GraphServiceClient<Request> graphClient = GraphServiceClient.builder()
				.authenticationProvider(tokenCredAuthProvider).buildClient();

		User me = null;
		try {
			me = graphClient.users(externalAccount.login).buildRequest().get();
		} catch (GraphServiceException e) {
			throw ServerFault.notFound("Teams user not found: " + externalAccount.login);
		}

		String confId = vevent.conferenceId;
		OnlineMeeting meeting;
		if (Strings.isNullOrEmpty(confId)) {
			OnlineMeeting onlineMeeting = new OnlineMeeting();
			onlineMeeting.subject = vevent.summary;
			LobbyBypassSettings lobbyBypass = new LobbyBypassSettings();
			lobbyBypass.scope = LobbyBypassScope.ORGANIZATION;
			lobbyBypass.isDialInBypassEnabled = false;
			onlineMeeting.lobbyBypassSettings = lobbyBypass;

			OnlineMeetingCollectionRequest req = graphClient.users(me.id).onlineMeetings().buildRequest();
			req.addHeader("Accept-Language", context.getSecurityContext().getLang());
			meeting = req.post(onlineMeeting);
		} else {
			OnlineMeetingRequest req = graphClient.users(me.id).onlineMeetings(confId).buildRequest();
			req.addHeader("Accept-Language", context.getSecurityContext().getLang());
			meeting = req.get();
		}

		String desc = meeting.joinInformation.content;
		Matcher dataUriMatcher = DATA_URI.matcher(desc);
		if (dataUriMatcher.find()) {
			String raw = dataUriMatcher.group(2);
			try {
				desc = URLDecoder.decode(raw, StandardCharsets.UTF_8.toString());
			} catch (UnsupportedEncodingException e) {
				// will not happen
			}
		}

		return new VideoConference(meeting.id, meeting.joinWebUrl, desc);
	}

	@Override
	public void deleteConference(BmContext context, Map<String, String> resourceSettings, String conferenceId) {
	}

	private Optional<String> resolveOrganizer(BmContext context, ICalendarElement vevent) {

		if (!Strings.isNullOrEmpty(vevent.organizer.dir)) {
			return Optional.of(IDirEntryPath.getEntryUid(vevent.organizer.dir.substring("bm://".length())));
		}

		if (!Strings.isNullOrEmpty(vevent.organizer.mailto)) {
			IDirectory directory = context.provider().instance(IDirectory.class,
					context.getSecurityContext().getContainerUid());
			DirEntry dirEntry = directory.getByEmail(vevent.organizer.mailto);
			if (dirEntry != null) {
				return Optional.of(dirEntry.entryUid);
			}
		}

		return Optional.empty();
	}

}
