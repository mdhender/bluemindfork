package net.bluemind.ui.adminconsole.system.domains.edit.mailflow.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;

public class UpdateSubjectConfig extends Composite implements MailflowActionConfig {
	private static final DomainConstants TEXTS = GWT.create(DomainConstants.class);

	Grid tbl = new Grid();

	public UpdateSubjectConfig() {
		tbl = new Grid(2, 2);
		tbl.setCellPadding(10);
		tbl.setWidget(0, 0, new Label(TEXTS.addSubjectPrefix()));
		tbl.setWidget(1, 0, new Label(TEXTS.addSubjectSuffix()));

		TextBox subjectPrefix = new TextBox();
		TextBox subjectSuffix = new TextBox();
		tbl.setWidget(0, 1, subjectPrefix);
		tbl.setWidget(1, 1, subjectSuffix);

		this.initWidget(tbl);
	}

	@Override
	public Map<String, String> get() {
		Map<String, String> values = new HashMap<>();
		values.put("subjectPrefix", ((TextBox) tbl.getWidget(0, 1)).getValue());
		values.put("subjectSuffix", ((TextBox) tbl.getWidget(1, 1)).getValue());

		return values;
	}

	@Override
	public void set(Map<String, String> config) {
		((TextBox) tbl.getWidget(0, 1)).setValue(config.get("subjectPrefix"));
		((TextBox) tbl.getWidget(1, 1)).setValue(config.get("subjectSuffix"));
	}

	@Override
	public String getIdentifier() {
		return "UpdateSubjectAction";
	}

	@Override
	public Widget getWidget() {
		return this;
	}
}
