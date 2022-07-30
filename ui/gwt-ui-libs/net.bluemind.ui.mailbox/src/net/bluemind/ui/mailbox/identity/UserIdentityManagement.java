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
package net.bluemind.ui.mailbox.identity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailflow.common.api.Recipient;
import net.bluemind.mailflow.common.api.Recipient.AddressType;
import net.bluemind.mailflow.common.api.Recipient.RecipientType;
import net.bluemind.mailflow.common.api.SendingAs;
import net.bluemind.mailmessage.api.IMailTipPromise;
import net.bluemind.mailmessage.api.MailTipContext;
import net.bluemind.mailmessage.api.MailTipFilter;
import net.bluemind.mailmessage.api.MailTipFilter.FilterType;
import net.bluemind.mailmessage.api.MailTips;
import net.bluemind.mailmessage.api.MessageContext;
import net.bluemind.mailmessage.api.gwt.endpoint.MailTipGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.UserMailIdentity;
import net.bluemind.user.api.gwt.endpoint.UserMailIdentitiesGwtEndpoint;

public class UserIdentityManagement extends IdentityManagement {

	public static final String TYPE = "bm.mailbox.UserIdentitiesEditor";
	private String userId;

	public static interface Resources extends ClientBundle {
		@Source("DomainSignature.css")
		Style domainSignatureStyle();
	}

	public static interface Style extends CssResource {

		String domainSig();

		String domainSigTitle();

		String spacerCell();

	}

	private static final Resources res = GWT.create(Resources.class);
	private final Style s;

	public UserIdentityManagement() {
		s = res.domainSignatureStyle();
	}

	@Override
	protected void loadIdentity(final IdentityDescription id, final AsyncHandler<UserMailIdentity> handler) {
		UserMailIdentitiesGwtEndpoint umi = new UserMailIdentitiesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid,
				userId);

		umi.get(id.id, handler);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		s.ensureInjected();
		final UserMailIdentitiesModel umim = model.cast();
		userId = umim.getUserId();
		super.loadModel(model);

		IMailTipPromise tipService = new MailTipGwtEndpoint(Ajax.TOKEN.getSessionId(), umim.getDomainUid())
				.promiseApi();

		String from = detectFrom();

		if (null != from) {

			MailTipContext mailtipContext = new MailTipContext();
			mailtipContext.filter = new MailTipFilter();
			mailtipContext.filter.filterType = FilterType.INCLUDE;
			mailtipContext.filter.mailTips = Arrays.asList("Signature");
			mailtipContext.messageContext = new MessageContext();
			mailtipContext.messageContext.fromIdentity = new SendingAs();
			mailtipContext.messageContext.fromIdentity.from = from;
			mailtipContext.messageContext.fromIdentity.sender = from;
			Recipient recipient = new Recipient();
			recipient.addressType = AddressType.SMTP;
			recipient.email = from;
			recipient.name = from;
			recipient.recipientType = RecipientType.TO;
			mailtipContext.messageContext.recipients = Arrays.asList(recipient);

			CompletableFuture<List<MailTips>> mailTipsInternal = tipService.getMailTips(mailtipContext);

			mailtipContext.messageContext.recipients.get(0).email = "out@somewhere.fr";
			CompletableFuture<List<MailTips>> mailTipsExternal = tipService.getMailTips(mailtipContext);
			mailTipsInternal.thenAccept(internal -> {
				mailTipsExternal.thenAccept(external -> {
					if (!internal.isEmpty() || !external.isEmpty()) {
						showSignatureInfo();
					}
				});
			});
		}
	}

	private String detectFrom() {
		for (IdentityDescription desc : super.identities) {
			if (super.defaultIdentity != null) {
				if (desc.id.equals(super.defaultIdentity)) {
					return desc.email;
				}
			} else {
				return desc.email;
			}
		}
		return null;
	}

	private void showSignatureInfo() {
		super.noIdentity.setVisible(true);
		super.noIdentity.setText(getTexts().domainSignaturePresent().asString());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		super.saveModel(model);
		final UserMailIdentitiesModel umim = model.cast();
		umim.setDefaultIdentity(defaultIdentity);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new UserIdentityManagement();
			}
		});

	}
}
