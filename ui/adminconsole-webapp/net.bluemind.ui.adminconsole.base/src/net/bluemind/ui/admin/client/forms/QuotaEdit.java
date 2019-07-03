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
package net.bluemind.ui.admin.client.forms;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.api.gwt.endpoint.MailboxesGwtEndpoint;
import net.bluemind.ui.admin.client.forms.l10n.quota.QuotaEditTexts;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.IFormChangeListener;
import net.bluemind.ui.common.client.forms.TrPanel;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

/**
 * Use this widget for string properties editing in crud forms
 * 
 * 
 */
public class QuotaEdit extends Composite implements IsEditor<ValueBoxEditor<Integer>>, ICommonEditor {

	public static interface Resources extends ClientBundle {

		@Source("QuotaEdit.css")
		Style stringEditStyle();

	}

	public static interface Style extends CssResource {

		String mandatory();

		String inputTitle();

		String vaTop();

		String inputQuota();

	}

	private static final Resources RES = GWT.create(Resources.class);
	private static final QuotaEditTexts TEXTS = GWT.create(QuotaEditTexts.class);

	private static final int MO_UNIT = 1024;
	private static final int GO_UNIT = 1024 * 1024;

	private IntegerBox text;
	private Label title;
	private Label mandatoryLabel;
	private boolean readOnly;
	private Style s;
	private TrPanel tr;
	private ListBox units;
	private RadioButton setQuotaTo;
	private RadioButton noQuota;
	private QuotaUsedIndicator quotaUsed;
	private Label quotaUsedLabel;

	private String propertyName;

	public QuotaEdit() {
		s = RES.stringEditStyle();
		s.ensureInjected();

		tr = new TrPanel();
		tr.addStyleName("setting");

		FlexTable ft = new FlexTable();

		title = new Label();
		tr.add(title, "label");

		FlowPanel quota = new FlowPanel();
		text = new IntegerBox();

		quota.add(text);
		text.setText("0");
		text.setTitle(TEXTS.tooltip());
		text.setStyleName("button-first");
		text.setWidth("5em");

		units = new ListBox();
		units.addItem(TEXTS.unitMiB(), "" + MO_UNIT);
		units.addItem(TEXTS.unitGiB(), "" + GO_UNIT);
		units.setStyleName("button-last");
		quota.add(units);

		String groupId = "quota_" + DOM.createUniqueId();
		setQuotaTo = new RadioButton(groupId, TEXTS.setQuotaTo());
		setQuotaTo.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				setEnable(true);
			}
		});

		ft.setWidget(0, 0, setQuotaTo);
		ft.setWidget(0, 1, quota);

		noQuota = new RadioButton(groupId, TEXTS.noQuota());
		noQuota.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				setEnable(false);
			}
		});
		ft.setWidget(1, 0, noQuota);
		ft.setWidget(1, 1, new Label(""));

		quotaUsed = new QuotaUsedIndicator();
		quotaUsedLabel = new Label(TEXTS.usedQuota());
		ft.setWidget(2, 0, quotaUsedLabel);
		ft.setWidget(2, 1, quotaUsed);

		tr.add(ft, "form");

		mandatoryLabel = new Label();
		tr.add(mandatoryLabel);
		initWidget(tr);

	}

	protected void setEnable(boolean b) {
		text.setEnabled(b);
		units.setEnabled(b);
		setQuotaTo.setValue(b);
		noQuota.setValue(!b);
		if (!b) {
			text.setValue(0);
			quotaUsed.setQuotaUsed(0, 0);
		}
	}

	public String getTitleText() {
		return title.getText();
	}

	@Override
	public void setTitleText(String titleText) {
		title.setText(titleText);
	}

	public int getMaxLength() {
		return text.getMaxLength();
	}

	public void setQuotaUsed(int usedPerc, int usedKb) {
		quotaUsed.setQuotaUsed(usedPerc, usedKb);
	}

	public void setMaxLength(int maxLength) {
		text.setMaxLength(maxLength);
	}

	public void setShowQuota(boolean b) {
		quotaUsed.setVisible(b);
		quotaUsedLabel.setVisible(b);
	}

	public void setMandatory(boolean b) {
		if (b) {
			mandatoryLabel.setText("*");
			tr.addStyleName(s.mandatory());
		} else {
			mandatoryLabel.setText("");
			tr.removeStyleName(s.mandatory());
		}
	}

	public boolean isMandatory() {
		return mandatoryLabel.getText().contains("*");
	}

	@Override
	public ValueBoxEditor<Integer> asEditor() {
		final ValueBoxEditor<Integer> ret = text.asEditor();
		final QuotaEdit self = this;
		ValueBoxEditor<Integer> div = new ValueBoxEditor<Integer>(text) {

			@Override
			public Integer getValue() {
				return self.getValue();
			}

			@Override
			public void setValue(Integer value) {
				GWT.log("setting value");
				self.setValue(value);
			}

			@Override
			public EditorDelegate<Integer> getDelegate() {
				return ret.getDelegate();
			}

			@Override
			public void setDelegate(EditorDelegate<Integer> delegate) {
				ret.setDelegate(delegate);
			}

		};
		return div;
	}

	private Integer getValue() {

		Integer val = text.asEditor().getValue();
		if (val == null) {
			return null;
		}
		switch (units.getSelectedIndex()) {
		case 1:
			// GO
			return val * GO_UNIT;

		case 0:
		default:
			// MO
			return val * MO_UNIT;
		}
	}

	public void setValue(Integer value) {
		// unit choice
		if (value == null) {
			setEnable(false);
		} else {
			if (value == 0) {
				units.setSelectedIndex(0);
				text.asEditor().setValue(0);
				setEnable(false);
			} else if (value > GO_UNIT && (value % GO_UNIT) == 0) {
				units.setSelectedIndex(1);
				text.asEditor().setValue(value / GO_UNIT);
				setEnable(true);
			} else {
				units.setSelectedIndex(0);
				text.asEditor().setValue(value / MO_UNIT);
				setEnable(true);
			}
		}

	}

	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;

		setQuotaTo.setEnabled(!readOnly);
		noQuota.setEnabled(!readOnly);
		text.setEnabled(!readOnly);
	}

	@Override
	public String getStringValue() {
		return "" + getValue();
	}

	@Override
	public void setStringValue(String v) {
		if (v != null) {
			setValue(Integer.parseInt(v));
		}
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	public void setDescriptionText(String s) {
	}

	@Override
	public Map<String, Widget> getWidgetsMap() {
		return null;
	}

	@Override
	public void addFormChangeListener(IFormChangeListener listener) {
	}

	@Override
	public void setId(String id) {
		units.getElement().setId(id + "-quota-units");
		text.getElement().setId(id + "-quota-value");
	}

	public void setMailboxAndDomain(String mailboxUid, String domainUid) {

		new MailboxesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).getMailboxQuota(mailboxUid,
				new AsyncHandler<MailboxQuota>() {

			@Override
			public void success(MailboxQuota value) {
				if (null == value.quota) {
					setQuotaUsed(0, 0);
				} else {
					double q = value.quota;
					double u = value.used;
					double perc = (u / q) * 100;
					int percInt = (int) perc;
					percInt = Math.min(100, percInt);
					setQuotaUsed((int) percInt, (int) u);
				}
			}

			@Override
			public void failure(Throwable e) {
				GWT.log("error retrieving quota usage", e);
				setQuotaUsed(0, 0);
			}
		});
	}
}
