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
package net.bluemind.ui.mailbox.filter.multipleforward;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.addressbook.api.VCardInfo;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.addressbook.api.gwt.endpoint.AddressBooksGwtEndpoint;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.ui.common.client.forms.Ajax;

public class MultipleForward extends Composite {
	public static interface Resources extends ClientBundle {
		@Source("MultipleForward.css")
		Style forwardMultipleStyle();
	}

	public static interface Style extends CssResource {
		String valuesPanel();

		String recipient();

		String disabled();

		String pointer();

		String recipientInput();
	}

	private static final Resources RES = GWT.create(Resources.class);

	public interface IChangeRecipients {
		public void onChangeRecipients();
	}

	private class RecipientSuggestion implements Suggestion {
		private String email;

		public RecipientSuggestion(String email) {
			this.email = email;
		}

		@Override
		public String getDisplayString() {
			return email;
		}

		@Override
		public String getReplacementString() {
			return email;
		}
	}

	private class emailSuggest extends SuggestOracle {
		@Override
		public void requestSuggestions(final Request request, final Callback callback) {
			VCardQuery query = new VCardQuery();
			query.size = 5;
			query.query = "(_exists_:value.communications.emails.value OR value.kind:group) AND (value.identification.formatedName.value:"
					+ request.getQuery() + " OR value.communications.emails.value:" + request.getQuery() + ")";
			new AddressBooksGwtEndpoint(Ajax.TOKEN.getSessionId()).search(query,
					new AsyncHandler<ListResult<ItemContainerValue<VCardInfo>>>() {
						@Override
						public void success(ListResult<ItemContainerValue<VCardInfo>> value) {
							String emailRegex = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@([a-z0-9-]+\\.)+[a-z]{2,}$";
							LinkedHashSet<RecipientSuggestion> suggestions = new LinkedHashSet<>();

							String loweredQuery = request.getQuery().toLowerCase();
							if (loweredQuery.matches(emailRegex)) {
								if (!recipientsEmails.contains(request.getQuery())) {
									suggestions.add(new RecipientSuggestion(loweredQuery));
								}
							}

							for (ItemContainerValue<VCardInfo> icv : value.values) {
								if (suggestions.size() > 5) {
									break;
								}

								if (icv.value.mail == null || icv.value.mail.trim().isEmpty()) {
									continue;
								}

								String loweredSuggestedEmail = icv.value.mail.toLowerCase();
								if (!loweredSuggestedEmail.matches(emailRegex)) {
									continue;
								}

								if (!recipientsEmails.contains(icv.value.mail)) {
									suggestions.add(new RecipientSuggestion(loweredSuggestedEmail));
								}
							}

							Response resp = new Response();
							resp.setSuggestions(suggestions);
							callback.onSuggestionsReady(request, resp);
						}

						@Override
						public void failure(Throwable e) {
						}
					});
		}

	}

	private class Recipient extends Composite {
		protected String recipient;
		private FlowPanel fp;

		public Recipient(String recipient) {
			this.recipient = recipient;

			fp = new FlowPanel();
			fp.setStyleName(style.recipient());

			Label del = new Label();
			del.addStyleName("fa");
			del.addStyleName("fa-times");
			del.addStyleName(style.pointer());
			del.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					removeRecipient(Recipient.this);
				}
			});
			fp.add(del);

			Label spacer = new Label();
			spacer.setWidth("5px");
			fp.add(spacer);

			Label l = new Label();
			l.setText(recipient.trim());
			fp.add(l);

			spacer = new Label();
			spacer.setWidth("5px");
			fp.add(spacer);

			initWidget(fp);
		}

		public void setEnabled(boolean b) {
			if (b) {
				fp.getWidget(0).addStyleName(style.pointer());
			} else {
				fp.getWidget(0).removeStyleName(style.pointer());
			}
		}
	};

	private final FlowPanel panel;
	private Set<String> recipientsEmails = new HashSet<String>();
	private Style style = RES.forwardMultipleStyle();
	private SuggestBox newRecipient;
	private List<IChangeRecipients> changeHandler = new ArrayList<IChangeRecipients>();

	public MultipleForward() {
		style.ensureInjected();

		panel = new FlowPanel();
		panel.addStyleName(style.valuesPanel());

		newRecipient = new SuggestBox(new emailSuggest());
		newRecipient.setStyleName(style.recipientInput());
		newRecipient.addSelectionHandler(new SelectionHandler<Suggestion>() {
			@Override
			public void onSelection(SelectionEvent<Suggestion> event) {
				addRecipient(event.getSelectedItem().getReplacementString());
				newRecipient.setValue("", false);
			}
		});
		panel.add(newRecipient);

		initWidget(panel);
	}

	public void setRecipients(Set<String> recipients) {
		this.recipientsEmails = recipients;

		while (panel.getWidgetCount() > 1) {
			panel.remove(0);
		}

		for (String e : recipients) {
			panel.insert(new Recipient(e), panel.getWidgetCount() - 1);
		}
	}

	private void addRecipient(String e) {
		if (recipientsEmails.contains(e)) {
			return;
		}

		recipientsEmails.add(e);
		panel.insert(new Recipient(e), panel.getWidgetCount() - 1);

		for (IChangeRecipients ch : changeHandler) {
			ch.onChangeRecipients();
		}
	}

	public Set<String> getRecipients() {
		return recipientsEmails;
	}

	private void removeRecipient(Recipient recipient) {
		if (recipient.recipient == null) {
			return;
		}

		recipientsEmails.remove(recipient.recipient);
		panel.remove(recipient);
	}

	public void setEnabled(boolean b) {
		newRecipient.setEnabled(b);

		if (b) {
			panel.removeStyleName(style.disabled());

			for (int i = 0; i < panel.getWidgetCount() - 1; i++) {
				Widget r = panel.getWidget(i);
				r.removeStyleName(style.disabled());
				((Recipient) r).setEnabled(b);
			}
		} else {
			panel.addStyleName(style.disabled());

			for (int i = 0; i < panel.getWidgetCount() - 1; i++) {
				Widget r = panel.getWidget(i);
				r.addStyleName(style.disabled());
				((Recipient) r).setEnabled(b);
			}
		}
	}

	public void addChangeHandler(IChangeRecipients changeHandler) {
		this.changeHandler.add(changeHandler);
	}
}
