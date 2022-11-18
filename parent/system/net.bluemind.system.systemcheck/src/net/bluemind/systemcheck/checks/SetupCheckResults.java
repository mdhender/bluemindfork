package net.bluemind.systemcheck.checks;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SetupCheckResults implements Iterable<CheckResult>, Serializable {

	private static final long serialVersionUID = 6392623819412369274L;
	private boolean ok;
	private List<CheckResult> list;
	private boolean error;
	private boolean devMode;
	private Map<String, String> dbInfos;

	public SetupCheckResults() {
		list = new LinkedList<>();
		ok = true;
		error = false;
		devMode = false;
	}

	public boolean isOk() {
		return ok;
	}

	public void add(CheckResult cr) {
		if (cr.getState() != CheckState.OK) {
			ok = false;
		}
		if (cr.getState() == CheckState.ERROR) {
			error = true;
		}
		list.add(cr);
	}

	@Override
	public Iterator<CheckResult> iterator() {
		return list.iterator();
	}

	public boolean isError() {
		return error;
	}

	public boolean isDevMode() {
		return devMode;
	}

	public void setDevMode(boolean devMode) {
		this.devMode = devMode;
	}

	public Map<String, String> getDbInfos() {
		return dbInfos;
	}

	public void setDbInfos(Map<String, String> dbInfos) {
		this.dbInfos = dbInfos;
	}

}
