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
package net.bluemind.ui.im.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface IMConstants extends Messages {
	public static final IMConstants INST = GWT.create(IMConstants.class);

	//
	String statusAvailable();

	String statusAway();

	String statusBusy();

	String statusNotAvailable();

	String statusOffline();

	String statusUnknown();

	String customStatusMessage();

	String searchContactPlaceholder();

	String newConversationButton();

	//
	String cancelButton();

	String joinButton();

	String declineButton();

	String sendButton();

	String inviteButton();

	String newInvitationPopupHeader();

	String newInvitationPopupContent();

	String inviteToGroupChat();

	String sendHistory();

	String sendTo();

	//
	String createConversationButton();

	String createConversationPlaceholder();

	//
	String loading();

	String favorites();

	String addToFavorites(String displayName);

	String removeFromFavorites(String displayName);

	String sendMessagePlaceholder();

	String closeConversationButton();

	String removeFromListButton();

	String notInList();

	String subscriptionRequest(String displayName);

	String addToFavoritesPopupHeader();

	String removeFromFavoritePopupHeader();

	String subriptionRequestPopupHeader();

	String hasJoinGroupChat(String displayName);

	String hasLeftGroupChat(String displayName);

	String hasInvitedYouToGroupChat(String displayName);

	String youHaveInvited(String displayName);

	String awSnap(String s);

	String groupChatHistoryWasSentTo(String email);

	String resendSubscriptionRequest();

	String sendGroupChatHistory();

	String customMessage();

	String customAvailableStatusMessage();

	String customBusyStatusMessage();

	String customAwayStatusMessage();

	String sendGroupChatHistoryError();

	String unreadMessages();

}
