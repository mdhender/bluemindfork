package net.bluemind.ui.adminconsole.directory.externaluser.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface ExternalUserConstants extends Messages {

	public ExternalUserConstants INST = GWT.create(ExternalUserConstants.class);

	public String delegation();

	public String lastName();

	public String firstName();

	public String email();

	public String hideFromGal();

	public String members();

	public String editTitle(String name);

	public String groups();

	public String editGroupMembership();
}
