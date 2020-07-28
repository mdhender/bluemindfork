package net.bluemind.ui.adminconsole.password.sizestrength;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;

import net.bluemind.custom.password.sizestrength.api.PasswordSizeStrenghtSettingsKeys;
import net.bluemind.custom.password.sizestrength.api.PasswordSizeStrengthDefaultValues;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.system.SettingsModel;

public class PasswordSizeStrengthSettingsEditor extends CompositeGwtWidgetElement {
	public static final String TYPE = "bm.ac.GlobalPasswordSizeStrengthEditor";

	private static PasswordSizeStrengthUiBinder uiBinder = GWT.create(PasswordSizeStrengthUiBinder.class);

	interface PasswordSizeStrengthUiBinder extends UiBinder<HTMLPanel, PasswordSizeStrengthSettingsEditor> {
	}

	@UiField
	CheckBox passwordSizeStrengthEnable;

	@UiField
	IntegerBox minLength;

	@UiField
	IntegerBox lower;

	@UiField
	IntegerBox capital;

	@UiField
	IntegerBox digit;

	@UiField
	IntegerBox punct;

	protected PasswordSizeStrengthSettingsEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		SettingsModel map = settingsModel(model);

		enablePolicySettings(
				Boolean.valueOf(map.get(PasswordSizeStrenghtSettingsKeys.password_sizestrength_enabled.name())));

		minLength.setValue(
				getSettingsValue(map.get(PasswordSizeStrenghtSettingsKeys.password_sizestrength_minimumlength.name()),
						PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_LENGTH));
		lower.setValue(getSettingsValue(map.get(PasswordSizeStrenghtSettingsKeys.password_sizestrength_lower.name()),
				PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_LOWER));
		capital.setValue(
				getSettingsValue(map.get(PasswordSizeStrenghtSettingsKeys.password_sizestrength_capital.name()),
						PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_CAPITAL));
		digit.setValue(getSettingsValue(map.get(PasswordSizeStrenghtSettingsKeys.password_sizestrength_digit.name()),
				PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_DIGIT));
		punct.setValue(getSettingsValue(map.get(PasswordSizeStrenghtSettingsKeys.password_sizestrength_punct.name()),
				PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_PUNCT));
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		SettingsModel map = settingsModel(model);
		map.putString(PasswordSizeStrenghtSettingsKeys.password_sizestrength_enabled.name(),
				Boolean.toString(passwordSizeStrengthEnable.getValue()));

		map.putString(PasswordSizeStrenghtSettingsKeys.password_sizestrength_minimumlength.name(),
				getSettingsValue(minLength.getValue(), PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_LENGTH)
						.toString());
		map.putString(PasswordSizeStrenghtSettingsKeys.password_sizestrength_lower.name(),
				getSettingsValue(lower.getValue(), PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_LOWER).toString());
		map.putString(PasswordSizeStrenghtSettingsKeys.password_sizestrength_capital.name(),
				getSettingsValue(capital.getValue(), PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_CAPITAL)
						.toString());
		map.putString(PasswordSizeStrenghtSettingsKeys.password_sizestrength_digit.name(),
				getSettingsValue(digit.getValue(), PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_DIGIT).toString());
		map.putString(PasswordSizeStrenghtSettingsKeys.password_sizestrength_punct.name(),
				getSettingsValue(punct.getValue(), PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_PUNCT).toString());
	}

	@UiHandler("passwordSizeStrengthEnable")
	void ldapImportChangeHandler(ClickEvent ce) {
		enablePolicySettings(((CheckBox) ce.getSource()).getValue());
	}

	private void enablePolicySettings(Boolean enabled) {
		passwordSizeStrengthEnable.setValue(enabled);

		minLength.setEnabled(enabled);
		lower.setEnabled(enabled);
		capital.setEnabled(enabled);
		digit.setEnabled(enabled);
		punct.setEnabled(enabled);
	}

	private String getSettingsValue(Integer value, Integer defaultMinimumPunct) {
		return value == null ? defaultMinimumPunct.toString() : value.toString();
	}

	private Integer getSettingsValue(String value, Integer defaultValue) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException nfe) {
			return defaultValue;
		}
	}

	protected SettingsModel settingsModel(JavaScriptObject model) {
		return SettingsModel.globalSettingsFrom(model);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {
			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new PasswordSizeStrengthSettingsEditor();
			}
		});
	}
}
