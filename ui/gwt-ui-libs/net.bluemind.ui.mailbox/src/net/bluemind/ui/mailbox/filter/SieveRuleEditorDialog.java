package net.bluemind.ui.mailbox.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.backend.mail.api.gwt.endpoint.ReadOnlyMailboxFoldersEndpointPromise;
import net.bluemind.backend.mail.api.gwt.endpoint.ReadOnlyMailboxFoldersSockJsEndpoint;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.mailbox.api.utils.RuleHandler;
import net.bluemind.mailbox.api.utils.RuleParser;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.DoneCancelActionBar;
import net.bluemind.ui.mailbox.filter.multipleforward.MultipleForward;

public class SieveRuleEditorDialog extends Composite {

	static class MoveToItem {
		String value;
		String name;

		public static MoveToItem create(String value, String name) {
			MoveToItem ret = new MoveToItem();
			ret.value = value;
			ret.name = name;
			return ret;
		}

		public static MoveToItem create(String value) {
			MoveToItem ret = new MoveToItem();
			ret.value = value;
			ret.name = value;
			return ret;
		}
	}

	static class MoveToMailbox {
		String uid;
		String mailboxName;
		String displayName;
		DirEntry.Kind kind;

		public static MoveToMailbox create(String uid, String mailboxName, String displayName, DirEntry.Kind kind) {
			MoveToMailbox ret = new MoveToMailbox();
			ret.uid = uid;
			ret.mailboxName = mailboxName;
			ret.displayName = displayName;
			ret.kind = kind;
			return ret;
		}
	}

	interface DialogHandler {
		public void validate(MailFilterRule value);

		public void cancel();
	}

	interface SieveRuleEditorDialogUiBinder extends UiBinder<DockLayoutPanel, SieveRuleEditorDialog> {

	}

	private static SieveRuleEditorDialogUiBinder uiBinder = GWT.create(SieveRuleEditorDialogUiBinder.class);

	private static final SieveConstants constants = GWT.create(SieveConstants.class);

	private static final DialogHandler EMPTY_HANDLER = new DialogHandler() {

		@Override
		public void validate(MailFilterRule value) {

		}

		@Override
		public void cancel() {
		}
	};

	private MultipleForward.IChangeRecipients checkFormOnChange = new MultipleForward.IChangeRecipients() {
		@Override
		public void onChangeRecipients() {
			checkForm();
		}
	};

	private final KeyUpHandler checkFormOnKeyStroke = new KeyUpHandler() {
		@Override
		public void onKeyUp(KeyUpEvent event) {
			checkForm();
		}
	};

	private final ClickHandler checkFormOnKeyClick = new ClickHandler() {

		@Override
		public void onClick(ClickEvent event) {
			checkForm();
		}
	};
	private DockLayoutPanel dlp;

	@UiField
	DoneCancelActionBar actionBar;

	@UiField
	Label filterFormTitle;

	@UiField
	TextBox from;

	@UiField
	TextBox to;

	@UiField
	TextBox subject;

	@UiField
	TextBox body;

	@UiField
	CheckBox cbMarkAsRead;

	@UiField
	CheckBox cbMarkAsImportant;

	@UiField
	CheckBox cbMoveTo;

	@UiField
	ListBox moveTo;

	@UiField
	CheckBox cbForwardTo;

	@UiField
	ListBox lbForwardToLocalCopy;

	@UiField
	MultipleForward forwardTo;

	@UiField
	ListBox fromMatchType;

	@UiField
	ListBox toMatchType;

	@UiField
	ListBox subjectMatchType;

	@UiField
	ListBox bodyMatchType;

	@UiField
	ListBox headerMatchType;

	@UiField
	TextBox header;

	@UiField
	TextBox headerValue;

	@UiField
	CheckBox cbDiscard;

	private enum Type {
		MAILSHARE, USER, DOMAIN
	};

	private final Type type;

	private String mbox;

	private DialogHandler handler;

	private MailFilterRule value;

	private Button okButton;

	private String domainUid;

	public static void openRuleEditor(String domainUid, MailFilterRule value, final DialogHandler handler,
			String entity, int entityId, String mbox, String datalocation) {
		final SieveRuleEditorDialog dialog = new SieveRuleEditorDialog(domainUid, entity, entityId, mbox, datalocation);

		dialog.setValue(value);
		dialog.setSize("800px", "500px");
		final DialogBox os = new DialogBox();
		os.addStyleName("dialog");

		os.setWidget(dialog);
		os.setGlassEnabled(true);
		os.setAutoHideEnabled(true);
		os.setGlassStyleName("modalOverlay");
		os.setModal(true);
		os.center();
		os.show();

		dialog.setHandler(new DialogHandler() {

			@Override
			public void validate(MailFilterRule value) {
				os.hide();
				handler.validate(value);

			}

			@Override
			public void cancel() {
				os.hide();
				handler.cancel();
			}

		});

	}

	private SieveRuleEditorDialog(String domainUid, String entity, int entityId, String mbox, String datalocation) {
		this.domainUid = domainUid;
		this.handler = EMPTY_HANDLER;
		this.mbox = mbox;

		if (entity.equals("mailshare")) {
			type = Type.MAILSHARE;
		} else if (entity.equals("user")) {
			type = Type.USER;
		} else if (entity.equals("domain")) {
			type = Type.DOMAIN;
		} else {
			throw new RuntimeException("Unsupported entity " + entity);
		}

		dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);

		okButton = actionBar.setDoneAction(new ScheduledCommand() {

			@Override
			public void execute() {
				handler.validate(getValue());
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {

			@Override
			public void execute() {
				handler.cancel();
			}
		});

		initUI();
	}

	private void setHandler(DialogHandler handler) {
		this.handler = handler;
	}

	private void initUI() {
		fromMatchType.getElement().setId("filter-from-select");
		from.getElement().setId("filter-from-text");
		toMatchType.getElement().setId("filter-to-select");
		to.getElement().setId("filter-to-text");
		subjectMatchType.getElement().setId("filter-subject-select");
		subject.getElement().setId("filter-subject-text");
		bodyMatchType.getElement().setId("filter-body-select");
		body.getElement().setId("filter-body-text");
		headerMatchType.getElement().setId("filter-header-select");
		header.getElement().setId("filter-header-text");
		headerValue.getElement().setId("filter-header-value-text");
		cbDiscard.getElement().setId("filter-discard-cb");
		cbForwardTo.getElement().setId("filter-forward-cb");
		cbMarkAsImportant.getElement().setId("filter-star-cb");
		cbMarkAsRead.getElement().setId("filter-read-cb");
		cbMoveTo.getElement().setId("filter-move-cb");
		moveTo.getElement().setId("filter-move-to-select");
		forwardTo.getElement().setId("filter-forward-to-text");

		lbForwardToLocalCopy.addItem(constants.forwardToWithLocalCopy(), "copy");
		lbForwardToLocalCopy.addItem(constants.forwardToWithoutLocalCopy(), "nocopy");

		initFolders();
		initHandlers();
	}

	private void initHandlers() {
		cbForwardTo.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				forwardTo.setEnabled(event.getValue());
				lbForwardToLocalCopy.setEnabled(event.getValue());
			}
		});
		forwardTo.setEnabled(cbForwardTo.getValue());
		lbForwardToLocalCopy.setEnabled(cbForwardTo.getValue());
		cbMoveTo.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				moveTo.setEnabled(event.getValue());
			}
		});
		moveTo.setEnabled(cbMoveTo.getValue());
		from.addKeyUpHandler(checkFormOnKeyStroke);
		to.addKeyUpHandler(checkFormOnKeyStroke);
		subject.addKeyUpHandler(checkFormOnKeyStroke);
		body.addKeyUpHandler(checkFormOnKeyStroke);

		cbMarkAsRead.addClickHandler(checkFormOnKeyClick);

		cbMarkAsImportant.addClickHandler(checkFormOnKeyClick);

		cbMoveTo.addClickHandler(checkFormOnKeyClick);

		cbForwardTo.addClickHandler(checkFormOnKeyClick);

		forwardTo.addChangeHandler(checkFormOnChange);

		header.addKeyUpHandler(checkFormOnKeyStroke);
		headerValue.addKeyUpHandler(checkFormOnKeyStroke);

		cbDiscard.addClickHandler(checkFormOnKeyClick);

		headerMatchType.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				headerValue.setValue(null);
				if (headerMatchType.getSelectedIndex() >= 4) {
					headerValue.setEnabled(false);
				} else {
					headerValue.setEnabled(true);
				}
			}
		});
	}

	private void checkForm() {
		if ((!from.getValue().isEmpty() || !to.getValue().isEmpty() || !subject.getValue().isEmpty()
				|| !body.getValue().isEmpty()
				|| (!header.getValue().isEmpty() && ((!headerValue.getValue().isEmpty() && headerValue.isEnabled())
						|| !headerValue.isEnabled())))
				&& (cbMarkAsRead.getValue() || cbMarkAsImportant.getValue() || cbDiscard.getValue()
						|| cbMoveTo.getValue() || (cbForwardTo.getValue() && forwardTo.getRecipients().size() != 0))) {
			// activate validate button
			okButton.setEnabled(true);
		} else {
			// desactivate validate button
			okButton.setEnabled(false);
		}
	}

	private void initFolders() {
		moveTo.addItem(constants.inbox(), "user:INBOX");
		moveTo.addItem(constants.sent(), "user:Sent");

		if (type == Type.DOMAIN) {
			moveTo.addItem(constants.trash(), "user:Trash");
			moveTo.addItem(constants.spam(), "user:Junk");
		} else {
			addMyFolders().thenAccept(o -> value.move()
					.ifPresent(move -> moveTo.setSelectedIndex(getItemByValue(moveTo, move.asString()))));
		}
	}

	private CompletableFuture<Void> addMyFolders() {
		String subtree = "subtree_" + domainUid.replace('.', '_') + "!" + (type == Type.USER ? "user." : "") + mbox;
		ReadOnlyMailboxFoldersEndpointPromise mf = new ReadOnlyMailboxFoldersEndpointPromise(
				new ReadOnlyMailboxFoldersSockJsEndpoint(Ajax.TOKEN.getSessionId(), subtree));
		return mf.all().thenAccept(folders -> {
			folders.stream().sorted((a, b) -> a.value.fullName.compareTo(b.value.fullName))
					.filter(folder -> !folder.flags.contains(ItemFlag.Deleted)
							&& !matches(folder.value.name, "inbox", "sent", "outbox"))
					.forEach(folder -> {
						String foldername = folder.value.fullName;
						if (foldername.equalsIgnoreCase("trash")) {
							moveTo.addItem(constants.trash(), "user:Trash");
						} else if (foldername.equalsIgnoreCase("junk")) {
							moveTo.addItem(constants.spam(), "user:Junk");
						} else if (foldername.equalsIgnoreCase("drafts")) {
							moveTo.addItem(constants.drafts(), "user:Drafts");
						} else {
							moveTo.addItem(folder.value.fullName,
									"user:" + folder.internalId + ":" + folder.value.fullName);
						}
					});
		});
	}

	private boolean matches(String folder, String... folders) {
		for (int i = 0; i < folders.length; i++) {
			if (folder.equalsIgnoreCase(folders[i])) {
				return true;
			}
		}

		return false;
	}

	private void resetForm() {
		from.setValue(null);
		to.setValue(null);
		subject.setValue(null);
		body.setValue(null);
		cbMarkAsRead.setValue(false);
		cbMarkAsImportant.setValue(false);
		cbMoveTo.setValue(false);
		moveTo.setEnabled(false);
		moveTo.setSelectedIndex(0);
		cbForwardTo.setValue(false);
		forwardTo.getRecipients().clear();
		fromMatchType.setSelectedIndex(0);
		toMatchType.setSelectedIndex(0);
		subjectMatchType.setSelectedIndex(0);
		bodyMatchType.setSelectedIndex(0);
		headerMatchType.setSelectedIndex(0);
		header.setValue(null);
		headerValue.setEnabled(true);
		headerValue.setValue(null);
		cbDiscard.setValue(false);

		filterFormTitle.setText(constants.newFilter());
	}

	public void setValue(MailFilterRule value) {
		resetForm();
		this.value = value;
		filterFormTitle.setText(constants.modifyFilter());
		RuleParser.visit(value, new RuleHandler() {

			@Override
			public void matches(String field, String value) {
				initFilterCriterion("MATCHES", field, value);
			}

			@Override
			public void isNot(String field, String value) {
				initFilterCriterion("ISNOT", field, value);
			}

			@Override
			public void is(String field, String value) {
				initFilterCriterion("IS", field, value);
			}

			@Override
			public void exists(String field) {
				initFilterCriterion("EXISTS", field, "");

			}

			@Override
			public void doesnotMatch(String field, String value) {
				initFilterCriterion("DOESNOTMATCH", field, value);
			}

			@Override
			public void doesnotExist(String field) {
				initFilterCriterion("DOESNOTEXIST", field, "");
			}

			@Override
			public void doesnotContain(String field, String value) {
				initFilterCriterion("DOESNOTCONTAIN", field, value);
			}

			@Override
			public void contains(String field, String value) {
				initFilterCriterion("CONTAINS", field, value);

			}

			private void initFilterCriterion(String smatch, String crit, String value) {
				if (crit.equals("FROM")) {
					initCombo(fromMatchType, smatch);
					from.setValue(value);
				} else if (crit.equals("TO")) {
					initCombo(toMatchType, smatch);
					to.setValue(value);
				} else if (crit.equals("SUBJECT")) {
					initCombo(subjectMatchType, smatch);
					subject.setValue(value);
				} else if (crit.equals("BODY")) {
					initCombo(bodyMatchType, smatch);
					body.setValue(value);
				} else {
					initCombo(headerMatchType, smatch);
					header.setValue(crit);
					headerValue.setValue(value);
				}
			}

			private void initCombo(ListBox combo, String smatch) {
				for (int i = 0; i < combo.getItemCount(); i++) {
					if (combo.getValue(i).equals(smatch)) {
						combo.setSelectedIndex(i);
						DomEvent.fireNativeEvent(Document.get().createChangeEvent(), combo);
						break;
					}
				}
			}

		});

		cbDiscard.setValue(value.markAsDeleted().isPresent());
		cbMarkAsRead.setValue(value.markAsRead().isPresent());
		cbMarkAsImportant.setValue(value.markAsImportant().isPresent());

		value.redirect().ifPresent(redirect -> {
			cbForwardTo.setValue(Boolean.TRUE);
			forwardTo.setRecipients(new HashSet<>(redirect.emails()));
			forwardTo.setEnabled(true);
			lbForwardToLocalCopy.setSelectedIndex(redirect.keepCopy() ? 0 : 1);
			lbForwardToLocalCopy.setEnabled(true);
		});

		value.move().ifPresent(move -> {
			cbMoveTo.setValue(Boolean.TRUE);
			moveTo.setEnabled(true);
			moveTo.setSelectedIndex(getItemByValue(moveTo, move.asString()));
		});
		checkForm();
	}

	public MailFilterRule getValue() {

		MailFilterRule rule = new MailFilterRule();
		rule.client = "bluemind";

		String fromValue = from.getValue().trim();
		if (!fromValue.isEmpty()) {
			String matchType = fromMatchType.getValue(fromMatchType.getSelectedIndex());
			rule.conditions.add(condition(matchType, fromValue, "from.email"));
		}

		String toValue = to.getValue().trim();
		if (!toValue.isEmpty()) {
			String matchType = toMatchType.getValue(toMatchType.getSelectedIndex());
			rule.conditions.add(condition(matchType, toValue, "to.email", "cc.email"));
		}

		String subjectValue = subject.getValue().trim();
		if (!subjectValue.isEmpty()) {
			String matchType = subjectMatchType.getValue(subjectMatchType.getSelectedIndex());
			rule.conditions.add(condition(matchType, subjectValue, "subject"));
		}

		String bodyValue = body.getValue().trim();
		if (!bodyValue.isEmpty()) {
			String matchType = bodyMatchType.getValue(bodyMatchType.getSelectedIndex());
			rule.conditions.add(condition(matchType, bodyValue, "part.content"));
		}

		String customHeader = header.getValue().trim();
		String customHeaderValue = headerValue.getValue().trim();
		if (!customHeader.isEmpty()) {
			String matchType = headerMatchType.getValue(headerMatchType.getSelectedIndex());
			rule.conditions.add(condition(matchType, customHeaderValue, "headers." + customHeader));
		}

		if (cbMarkAsRead.getValue()) {
			rule.addMarkAsRead();
		}
		if (cbMarkAsImportant.getValue()) {
			rule.addMarkAsImportant();
		}
		if (cbDiscard.getValue()) {
			rule.addMarkAsDeleted();
		}
		if (cbMoveTo.getValue()) {
			rule.addMoveFromString(moveTo.getValue(moveTo.getSelectedIndex()));
		}

		if (cbForwardTo.getValue()) {
			boolean keepCopy = lbForwardToLocalCopy.getSelectedValue().equals("copy");
			rule.addRedirect(new ArrayList<>(forwardTo.getRecipients()), keepCopy);
		}
		rule.active = value.active;
		return rule;
	}

	private MailFilterRuleCondition condition(String matchType, String value, String... fields) {
		List<String> parameters = value.isEmpty() ? Collections.emptyList() : Arrays.asList(value);
		MailFilterRuleCondition condition = condition(matchType, Arrays.asList(fields), parameters);
		return (isException(matchType)) ? condition.not() : condition;
	}

	private MailFilterRuleCondition condition(String matchType, List<String> fields, List<String> parameters) {
		switch (matchType) {
		case "EXISTS":
		case "DOESNOTEXIST":
			return MailFilterRuleCondition.exists(fields);
		case "IS":
		case "ISNOT":
			return MailFilterRuleCondition.equal(fields, parameters);
		case "CONTAINS":
		case "DOESNOTCONTAIN":
			return MailFilterRuleCondition.contains(fields, parameters);
		case "MATCHES":
		case "DOESNOTMATCH":
			return MailFilterRuleCondition.matches(fields, parameters);
		default:
			return null;
		}
	}

	private boolean isException(String matchType) {
		return matchType.startsWith("DOESNOT") || matchType.endsWith("NOT");
	}

	private int getItemByValue(ListBox listBox, String value) {
		int indexToFind = -1;
		for (int i = 0; i < listBox.getItemCount(); i++) {
			if (listBox.getValue(i).equals(value)) {
				indexToFind = i;
				break;
			}
		}
		return indexToFind;
	}

}
