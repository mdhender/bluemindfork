package net.bluemind.ui.adminconsole.directory.externaluser;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.api.gwt.js.JsEmail;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.externaluser.api.gwt.endpoint.ExternalUserGwtEndpoint;
import net.bluemind.externaluser.api.gwt.js.JsExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.adminconsole.directory.EditGroupMembership;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.StringEdit;

public class EditExternalUser extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.ExternalUserEditor";

	interface EditExternalUserUiBinder extends UiBinder<HTMLPanel, EditExternalUser> {
	}

	private static EditExternalUserUiBinder uiBinder = GWT.create(EditExternalUserUiBinder.class);

	private String domainUid;
	private String externalUserUid;

	@UiField
	StringEdit email;

	@UiField
	DelegationEdit delegation;

	@UiField
	ListBox groups;

	@UiField
	Anchor editGroupMembership;

	@UiField
	CheckBox hideExtUser;

	protected EditExternalUser() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);

		editGroupMembership.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				event.preventDefault();

				EditGroupMembership g = new EditGroupMembership(domainUid, externalUserUid, Kind.EXTERNALUSER);
				g.registerObserver(EditExternalUser.this);
				SizeHint sh = g.getSizeHint();
				g.setSize(sh.getWidth() + "px", sh.getHeight() + "px");

				DialogBox overlay = new DialogBox();
				overlay.addStyleName("dialog");
				g.setOverlay(overlay);
				overlay.setWidget(g);
				overlay.setGlassEnabled(true);
				overlay.setAutoHideEnabled(true);
				overlay.setGlassStyleName("modalOverlay");
				overlay.setModal(true);
				overlay.center();
				overlay.show();
			}
		});

	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditExternalUser();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JsExternalUser externalUser = map.get("externaluser").cast();
		externalUserUid = map.getString("externalUserId");
		domainUid = map.getString("domainUid");
		email.asEditor().setValue(externalUser.getEmails().get(0).getAddress());
		delegation.setDomain(domainUid);
		delegation.asEditor().setValue(externalUser.getOrgUnitUid());
		reloadGroups();
		groups.setMultipleSelect(true);
		groups.setEnabled(false);
		hideExtUser.asEditor().setValue(externalUser.getHidden());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsExternalUser externalUser = map.get("externaluser").cast();

		externalUser.setOrgUnitUid(delegation.asEditor().getValue());
		externalUser.setHidden(hideExtUser.asEditor().getValue());

		// email
		JsArray<JsEmail> emails = JsArray.createArray().cast();
		JsEmail euEmail = JsEmail.create();
		euEmail.setAddress(email.asEditor().getValue());
		euEmail.setIsDefault(true);
		euEmail.setAllAliases(false);
		emails.push(euEmail);
		externalUser.setEmails(emails);
	}

	@Override
	public void onBrowserEvent(Event event) {
		// FIXME ugly, dishonorable
		if (null == event) {
			reloadGroups();
		} else {
			super.onBrowserEvent(event);
		}
	}

	private void reloadGroups() {
		for (int i = 0; i < groups.getItemCount(); i++) {
			groups.removeItem(i);
		}

		new ExternalUserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).memberOf(externalUserUid,
				new DefaultAsyncHandler<List<ItemValue<Group>>>() {

			@Override
			public void success(List<ItemValue<Group>> value) {
				for (ItemValue<Group> itemValue : value) {
					groups.addItem(itemValue.displayName);
				}
			}

		});
	}
}
