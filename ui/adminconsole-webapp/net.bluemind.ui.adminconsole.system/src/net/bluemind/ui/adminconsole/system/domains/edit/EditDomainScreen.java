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
package net.bluemind.ui.adminconsole.system.domains.edit;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.CrudActionBar;
import net.bluemind.ui.adminconsole.system.domains.edit.bmservices.EditDomainBmServicesEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.filters.EditDomainFiltersEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.filters.FiltersModelHandler;
import net.bluemind.ui.adminconsole.system.domains.edit.general.DomainMaxBasicAccountEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.general.DomainMaxUserEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.general.EditDomainGeneralEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.indexing.EditDomainIndexingEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.EditMailflowRulesEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.mailsystem.EditDomainMailsystemEditor;
import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;
import net.bluemind.ui.gwttag.client.DomainTagsEditor;
import net.bluemind.ui.gwttag.client.DomainTagsModelHandler;

public class EditDomainScreen extends Composite implements IGwtCompositeScreenRoot {

	public static final String TYPE = "bm.ac.EditDomainScreen";
	@UiField
	SimplePanel center;

	@UiField
	CrudActionBar actionBar;

	private ScreenRoot screenRoot;

	interface EditDomainScreenUiBinder extends UiBinder<DockLayoutPanel, EditDomainScreen> {
	}

	private static EditDomainScreenUiBinder uiBinder = GWT.create(EditDomainScreenUiBinder.class);

	private EditDomainScreen(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		actionBar.setCancelAction(getCancelAction());
		actionBar.setSaveAction(getSaveAction());
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new EditDomainScreen(screenRoot);
			}
		});
	}

	public Element getCenter() {
		return center.getElement();
	}

	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();
	}

	@Override
	public void doLoad(final ScreenRoot instance) {
		instance.load(new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				instance.loadModel(instance.getModel());
			}

			@Override
			public void failure(Throwable e) {
				GWT.log("Error occured while loading edit domain screen: " + e);
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

	private ScheduledCommand getSaveAction() {
		return new ScheduledCommand() {

			@Override
			public void execute() {
				screenRoot.save(new DefaultAsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						Actions.get().showWithParams2("domainsManager", null);
					}
				});
			}
		};
	}

	private ScheduledCommand getCancelAction() {
		return new ScheduledCommand() {

			@Override
			public void execute() {
				History.back();
			}
		};
	}

	public static ScreenElement screenModel() {
		DomainConstants c = DomainConstants.INST;
		ScreenRoot screenRoot = ScreenRoot.create("editDomain", TYPE).cast();
		screenRoot.getHandlers().push(ModelHandler.create(null, DomainModelHandler.TYPE).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, DomainSettingsModelHandler.TYPE).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, ServersModelHandler.TYPE).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, FiltersModelHandler.TYPE).<ModelHandler>cast());
		screenRoot.getHandlers()
				.push(ModelHandler.create(null, DomainAssignmentsModelHandler.TYPE).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, DomainTagsModelHandler.TYPE).<ModelHandler>cast());

		JsArray<Tab> tabs = JavaScriptObject.createArray().cast();

		JsArray<ScreenElement> editDomainGeneralContents = JsArray.createArray().cast();
		editDomainGeneralContents.push(ScreenElement.create(null, EditDomainGeneralEditor.TYPE));
		editDomainGeneralContents.push(ScreenElement.create(null, DomainMaxUserEditor.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_DOMAIN_MAX_VALUES));
		editDomainGeneralContents.push(ScreenElement.create(null, DomainMaxBasicAccountEditor.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_DOMAIN_MAX_VALUES));
		ContainerElement editDomainGeneral = ContainerElement.create("editDomainGeneral", editDomainGeneralContents);
		tabs.push(Tab.create(null, c.generalTab(), editDomainGeneral));

		tabs.push(Tab.create(null, c.filtersTab(),
				ScreenElement.create("editDomainFilters", EditDomainFiltersEditor.TYPE)));
		tabs.push(Tab.create(null, c.tagsTab(), ScreenElement.create("editDomainTags", DomainTagsEditor.TYPE)));
		tabs.push(Tab.create(null, c.mailSystemTab(),
				ScreenElement.create("editDomainMailSystem", EditDomainMailsystemEditor.TYPE)));
		tabs.push(Tab.create(null, c.mailflowRules(),
				ScreenElement.create("editMailflowRules", EditMailflowRulesEditor.TYPE)));
		tabs.push(Tab.create(null, c.indexingTab(),
				ScreenElement.create("editDomainIndexing", EditDomainIndexingEditor.TYPE)));

		JsArray<ScreenElement> children = JsArray.createArray().cast();
		children.push(ScreenElement.create(null, EditDomainBmServicesEditor.TYPE));
		ContainerElement domainServiceContainerElement = ContainerElement.create("editDomainBmServices", children);

		tabs.push(Tab.create(null, c.bmServicesTab(), domainServiceContainerElement));

		TabContainer tab = TabContainer.create("editDomainTabs", tabs);
		screenRoot.setContent(tab);
		return screenRoot;
	}

}
