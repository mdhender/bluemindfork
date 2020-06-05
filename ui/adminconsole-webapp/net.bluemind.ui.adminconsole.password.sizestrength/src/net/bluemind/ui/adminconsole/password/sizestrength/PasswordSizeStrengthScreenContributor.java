package net.bluemind.ui.adminconsole.password.sizestrength;

import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.ui.adminconsole.password.sizestrength.l10n.PasswordSizeStrength;

public class PasswordSizeStrengthScreenContributor implements ScreenElementContributorUnwrapper {

	@Override
	public JsArray<ScreenElementContribution> contribution() {
		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();

		ScreenElementContribution passwordSizeStrengthTabDomainConfig = ScreenElementContribution.create("editSystemConfTabs",
				"tabs", Tab.create("passwordSizeStrengthTab", PasswordSizeStrength.INST.tabName(),
						ScreenElement.create(null, PasswordSizeStrengthSettingsEditor.TYPE)));

		contribs.push(passwordSizeStrengthTabDomainConfig);
		return contribs;
	}

}
