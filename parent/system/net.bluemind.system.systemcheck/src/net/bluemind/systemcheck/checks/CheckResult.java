package net.bluemind.systemcheck.checks;

import java.io.Serializable;

public class CheckResult implements Serializable {

	private static final long serialVersionUID = 8588727421970993049L;
	private CheckState state;
	private String titleKey;
	private String reasonKey;

	public CheckResult() {
		state = CheckState.OK;
	}

	public CheckState getState() {
		return state;
	}

	public void setState(CheckState state) {
		this.state = state;
	}

	public String getTitleKey() {
		return titleKey;
	}

	public void setTitleKey(String titleKey) {
		this.titleKey = titleKey;
	}

	public String getReasonKey() {
		return reasonKey;
	}

	public void setReasonKey(String reasonKey) {
		this.reasonKey = reasonKey;
	}

}
