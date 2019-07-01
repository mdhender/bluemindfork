/* BEGIN LICENSE
 * Copyright Â© Blue Mind SAS, 2012-2016
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
package net.bluemind.ui.gwtsharing.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.calendar.api.IPublishCalendarPromise;
import net.bluemind.calendar.api.PublishMode;
import net.bluemind.calendar.api.gwt.endpoint.PublishCalendarGwtEndpoint;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectoryAsync;
import net.bluemind.directory.api.gwt.endpoint.DirectorySockJsEndpoint;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.CommonForm;
import net.bluemind.ui.common.client.forms.CrudConstants;
import net.bluemind.ui.common.client.forms.acl.AclAutoComplete;
import net.bluemind.ui.common.client.forms.acl.AclCombo;
import net.bluemind.ui.common.client.forms.acl.AclConstants;
import net.bluemind.ui.common.client.forms.acl.AclEntity;
import net.bluemind.ui.common.client.forms.acl.IEntitySelectTarget;
import net.bluemind.ui.common.client.icon.Trash;

public class AclEdit extends CommonForm implements IEntitySelectTarget {

	@UiField
	FlexTable table;

	@UiField
	FlowPanel publicComboContainer;

	@UiField
	FlowPanel publicAddressContainer;

	@UiField
	FlowPanel privateAddressContainer;

	@UiField
	FlowPanel externalContainer;

	@UiField
	AclAutoComplete autocomplete;

	@UiField
	Label noSharing;

	public static interface Resources extends ClientBundle {

		@Source("AclEdit.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {

		String aclContainer();

		String name();

		String trash();

		String info();

		String warning();

		String warningIcon();

		String warningDialogContent();

	}

	private static final Resources res = GWT.create(Resources.class);
	private static final AclConstants constants = GWT.create(AclConstants.class);
	private static AclUiBinder uiBinder = GWT.create(AclUiBinder.class);
	private static final CrudConstants cc = GWT.create(CrudConstants.class);

	interface AclUiBinder extends UiBinder<HTMLPanel, AclEdit> {
	}

	private IDirectoryAsync directory;
	private final Style s;
	private CheckBox publicCheckbox;
	private AclCombo publicCombo;
	private Map<String, String> verbs;
	private HashMap<AclEntity, AclCombo> entities;
	public String domainUid;
	private String containerUid;
	private FlexTable pubAddress;
	private FlexTable privAddress;

	private AbstractDirEntryOpener opener;

	private List<IAclEntityValidator> validators = new ArrayList<>();

	public AclEdit(Map<String, String> verbs, AbstractDirEntryOpener opener) {
		super();
		this.opener = opener;
		s = res.editStyle();
		s.ensureInjected();

		this.verbs = verbs;
		entities = new HashMap<AclEntity, AclCombo>();

		form = uiBinder.createAndBindUi(this);

		table.setVisible(false);
		noSharing.setVisible(false);

		publicCombo = new AclCombo(verbs);
		publicCombo.getElement().setId("acl-edit-public-combo");
		publicCombo.setEnable(false);

		publicCheckbox = new CheckBox(constants.aclAllowPublic());
		publicCheckbox.getElement().setId("acl-edit-public-checkbox");
		publicCheckbox.setValue(false);

		publicCheckbox.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				publicCombo.setEnable(publicCheckbox.getValue());
			}
		});
		FlexTable pub = new FlexTable();
		pub.setWidget(0, 0, publicCheckbox);
		pub.setWidget(1, 0, publicCombo);
		publicComboContainer.add(pub);

		// Public address
		pubAddress = new FlexTable();
		Button pubButton = new Button(constants.allowPublicAddress());

		pubButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				IPublishCalendarPromise service = new PublishCalendarGwtEndpoint(Ajax.TOKEN.getSessionId(),
						containerUid).promiseApi();
				service.generateUrl(PublishMode.PUBLIC).thenRun(() -> reloadPublishedCalendarUrls());
			}
		});

		pubButton.getElement().setId("acl-edit-public-address-checkbox");
		pubAddress.setWidget(0, 0, pubButton);
		Label publicLabel = new Label(constants.publicAddressDesc());
		pubAddress.setWidget(1, 0, publicLabel);
		publicAddressContainer.add(pubAddress);

		// Private address
		privAddress = new FlexTable();
		Button privButton = new Button(constants.allowPrivateAddress());

		privButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				IPublishCalendarPromise service = new PublishCalendarGwtEndpoint(Ajax.TOKEN.getSessionId(),
						containerUid).promiseApi();
				service.generateUrl(PublishMode.PRIVATE).thenRun(() -> reloadPublishedCalendarUrls());
			}
		});

		privButton.getElement().setId("acl-edit-private-address-checkbox");
		privAddress.setWidget(0, 0, privButton);
		Label privLabel = new Label(constants.privateAddressDesc());
		privAddress.setWidget(1, 0, privLabel);
		privateAddressContainer.add(privAddress);

		table.setStyleName(s.aclContainer());

		autocomplete.setTarget(this);
		autocomplete.getElement().setId("acl-edit-autocomplete");
	}

	public void setAddressesSharing(String containerType) {
		externalContainer.setVisible(true);
	}

	public void setVerbs(Map<String, String> verbs) {
		this.verbs = verbs;
		publicCombo.setVerbs(verbs);
	}

	public void setDomainUid(String domainUid) {
		directory = new DirectorySockJsEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		this.domainUid = domainUid;
		autocomplete.setDomain(domainUid);
	}

	@Override
	public void seletected(AclEntity aclEntity) {
		boolean present = false;
		for (AclEntity entry : entities.keySet()) {
			if (entry.getEntry().entryUid.equals(aclEntity.getEntry().entryUid)) {
				present = true;
				break;
			}
		}
		if (!present) {
			Optional<ValidationResult> error = validate(aclEntity);
			if (error.isPresent()) {
				aclEntityWarning(error.get().getErrorMessage());
			} else {
				addEntry(aclEntity);
			}
		}

	}

	private Optional<ValidationResult> validate(AclEntity aclEntity) {
		return validators.stream().map(validator -> validator.validate(aclEntity)).filter(result -> !result.isValid())
				.findFirst();
	}

	public void setValue(List<AccessControlEntry> entries) {
		if (directory == null) {
			throw new RuntimeException("domainUid is not defined");
		}

		entities = new HashMap<AclEntity, AclCombo>();
		table.removeAllRows();
		publicCombo.setEnable(false);
		publicCombo.setValue(Verb.Invitation); // default verb
		publicCheckbox.setValue(false);
		hasSharing(false);
		for (final AccessControlEntry a : entries) {
			if (a.subject.equals(domainUid)) {
				publicCombo.setValue(a.verb);
				publicCombo.setEnable(true);
				publicCheckbox.setValue(true);
			} else {

				directory.findByEntryUid(a.subject, new DefaultAsyncHandler<DirEntry>() {

					@Override
					public void success(DirEntry value) {
						if (value != null) {
							AclEntity ae = new AclEntity(value, a.verb);
							addEntry(ae);
						}

					}
				});
			}
		}

		reloadPublishedCalendarUrls();
	}

	private void reloadPublishedCalendarUrls() {
		if (!containerUid.contains("calendar")) {
			return;
		}
		IPublishCalendarPromise service = new PublishCalendarGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid)
				.promiseApi();

		loadUrls(service, PublishMode.PUBLIC, pubAddress);
		loadUrls(service, PublishMode.PRIVATE, privAddress);
	}

	private void loadUrls(IPublishCalendarPromise service, PublishMode mode, FlexTable tbl) {
		if (tbl.getRowCount() > 1) {
			for (int i = tbl.getRowCount() - 1; i >= 1; i--) {
				tbl.removeRow(i);
			}
		}

		service.getGeneratedUrls(mode).thenAccept(urls -> {
			for (int i = 0; i < urls.size(); i++) {
				Label url = new Label(urls.get(i));
				url.setHeight("35px");
				url.setStyleName("");
				tbl.setWidget(i + 1, 0, url);
				Button trash = new Button();
				trash.setStyleName("fa fa-trash-o");
				tbl.setWidget(i + 1, 1, trash);
				final int index = i;
				trash.addClickHandler(new ClickHandler() {

					@Override
					public void onClick(ClickEvent event) {
						service.disableUrl(urls.get(index)).thenRun(() -> reloadPublishedCalendarUrls());
					}
				});
			}
		});
	}

	public List<AccessControlEntry> getValue() {
		List<AclEntity> entities = getEntries();
		List<AccessControlEntry> acl = new ArrayList<>(entities.size());
		for (AclEntity entity : entities) {
			acl.add(AccessControlEntry.create(entity.getEntry().entryUid, entity.getVerb()));
		}
		return acl;
	}

	private void hasSharing(boolean hasSharing) {
		table.setVisible(hasSharing);
		noSharing.setVisible(!hasSharing);
	}

	private void addEntry(final AclEntity a) {
		final String key = a.getEntry().entryUid;

		AclCombo combo = new AclCombo(verbs);
		combo.getElement().setId("acl-edit-entry-" + key);
		if (combo.isValidValue(a.getVerb())) {
			Optional<ValidationResult> error = validate(a);
			int row = table.getRowCount();
			combo.setValue(a.getVerb());
			Label icon = new Label();
			if (a.getEntry().kind == Kind.USER) {
				icon.setStyleName("fa fa-lg fa-user");
			} else {
				icon.setStyleName("fa fa-lg fa-users");
			}
			Trash trash = new Trash();
			trash.setId("acl-edit-trash-" + key);
			trash.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					entities.remove(a);
					Cell c = table.getCellForEvent(event);
					table.removeRow(c.getRowIndex());
					hasSharing(table.getRowCount() > 0);
				}
			});

			Label warning = new Label();
			warning.setStyleName("fa fa-lg fa-exclamation-circle");
			warning.addStyleName(s.warningIcon());
			warning.setVisible(false);

			entities.put(a, combo);
			table.setWidget(row, 0, icon);
			Label name = new Label(a.getEntry().displayName);
			name.setTitle(a.getEntry().displayName);
			table.setWidget(row, 1, name);
			table.setWidget(row, 2, combo);
			table.setWidget(row, 3, warning);
			table.setWidget(row, 4, trash);
			table.getCellFormatter().setStyleName(row, 1, s.name());
			table.getCellFormatter().setStyleName(row, 3, s.warning());
			table.getCellFormatter().setStyleName(row, 4, s.trash());

			if (error.isPresent()) {
				warning.setTitle(error.get().getErrorMessage());
				warning.setVisible(true);
			}
			if (opener != null) {
				name.getElement().getStyle().setCursor(Cursor.POINTER);
				name.addClickHandler(e -> {
					opener.open(domainUid, a.getEntry());
				});

			}
		}

		hasSharing(!entities.isEmpty());
	}

	private List<AclEntity> getEntries() {
		List<AclEntity> values = new LinkedList<AclEntity>();

		// Public
		if (publicCheckbox.getValue()) {
			DirEntry dir = new DirEntry(); // FIXME public
			dir.entryUid = domainUid;
			AclEntity pub = new AclEntity(dir, publicCombo.getValue());
			values.add(pub);
		}

		// Specific
		for (AclEntity a : entities.keySet()) {
			AclCombo v = entities.get(a);
			AclEntity e = new AclEntity(a.getEntry(), v.getValue());
			values.add(e);
		}

		return values;
	}

	/**
	 * @param items
	 * @param sb
	 */
	private void aclEntityWarning(String errorMessage) {
		DialogBox os = new DialogBox();

		FlowPanel buttons = new FlowPanel();
		Button ok = new Button(cc.done());
		ok.addStyleName("button");
		ok.addStyleName("primary");
		ok.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				os.hide();
			}
		});

		buttons.add(ok);
		buttons.getElement().getStyle().setPadding(5, Unit.PX);

		DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);
		dlp.setHeight("150px");
		dlp.setWidth("500px");

		Label warn = new Label(ErrorCodeTexts.INST.PERMISSION_DENIED());
		warn.addStyleName("modal-dialog-title");
		dlp.addNorth(warn, 40);

		Label content = new Label(errorMessage);
		content.addStyleName(s.warningDialogContent());
		dlp.addSouth(buttons, 40);

		dlp.add(content);

		os.addStyleName("dialog");
		os.getElement().setAttribute("style", "padding:0");
		os.setWidget(dlp);
		os.setGlassEnabled(true);
		os.setAutoHideEnabled(false);
		os.setGlassStyleName("modalOverlay");
		os.setModal(true);
		os.center();
		os.show();
	}

	public void setEnable(boolean e) {
		publicCheckbox.setEnabled(e);
		table.removeAllRows();
		autocomplete.setEnable(e);
	}

	public void setVisible(boolean b) {
		form.setVisible(b);
	}

	public void setContainerUid(String containerUid) {
		this.containerUid = containerUid;
	}

	public String getContainerUid() {
		return this.containerUid;
	}

	public void setPublicSharingVisible(boolean b) {
		publicComboContainer.setVisible(b);
	}

	public void registerValidator(IAclEntityValidator validator) {
		this.validators.add(validator);
	}

}