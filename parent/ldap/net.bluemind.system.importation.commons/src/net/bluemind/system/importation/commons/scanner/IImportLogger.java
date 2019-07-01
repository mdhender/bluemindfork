package net.bluemind.system.importation.commons.scanner;

import java.util.Map;

public interface IImportLogger {
	public void info(Map<String, String> messages);

	public void warning(Map<String, String> messages);

	public void error(Map<String, String> messages);

	public void reportException(Throwable t);

	public ImportLogger withoutStatus();
}
