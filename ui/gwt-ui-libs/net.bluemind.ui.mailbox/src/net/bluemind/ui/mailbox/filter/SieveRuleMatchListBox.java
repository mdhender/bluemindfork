package net.bluemind.ui.mailbox.filter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ListBox;

public class SieveRuleMatchListBox extends ListBox {

	private static final SieveConstants constants = GWT.create(SieveConstants.class);

	private boolean allRules;

	public boolean isAllRules() {
		return allRules;
	}

	public void setAllRules(boolean allRules) {
		this.allRules = allRules;
		initItems();
	}

	public SieveRuleMatchListBox() {
		initItems();
	}

	private void initItems() {
		clear();
		addItem(constants.is(), "IS");
		addItem(constants.isNot(), "ISNOT");
		addItem(constants.contains(), "CONTAINS");
		addItem(constants.doesNotContain(), "DOESNOTCONTAIN");
		if (allRules) {
			addItem(constants.exists(), "EXISTS");
			addItem(constants.doesNotExist(), "DOESNOTEXIST");
		}
	}
}
