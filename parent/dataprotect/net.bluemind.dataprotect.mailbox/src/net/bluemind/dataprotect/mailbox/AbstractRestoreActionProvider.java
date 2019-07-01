package net.bluemind.dataprotect.mailbox;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableMap;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.dataprotect.api.RestoreOperation;
import net.bluemind.dataprotect.mailbox.internal.MboxRestoreService.Mode;
import net.bluemind.dataprotect.mailbox.internal.RestoreBoxTask;
import net.bluemind.dataprotect.service.IRestoreActionProvider;

public abstract class AbstractRestoreActionProvider implements IRestoreActionProvider {

	private final RestorableKind kind;

	public AbstractRestoreActionProvider(RestorableKind kind) {
		this.kind = kind;
	}

	@Override
	public TaskRef run(RestoreOperation op, DataProtectGeneration backup, Restorable item)
			throws ServerFault {

		ITasksManager tsk = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITasksManager.class);
		IServerTask toRun = null;
		switch (op.identifier) {
		case "replace.mailbox":
			toRun = new RestoreBoxTask(backup, item, Mode.Replace);
			break;
		case "subfolder.mailbox":
			toRun = new RestoreBoxTask(backup, item, Mode.Subfolder);
			break;
		default:
			throw new ServerFault("Unsupported op identifier: " + op.identifier);
		}
		return tsk.run(toRun);
	}

	@Override
	public List<RestoreOperation> operations() {
		RestoreOperation replace = new RestoreOperation();
		replace.identifier = "replace.mailbox";
		replace.translations = ImmutableMap.of("en", "Replace Mailbox", "fr", "Remplacer la boite mail");
		replace.kind = kind;
		replace.requiredTag = "mail/imap";

		RestoreOperation sub = new RestoreOperation();
		sub.identifier = "subfolder.mailbox";
		sub.translations = ImmutableMap.of("en", "Restore Mailbox in Subfolder", //
				"fr", "Restaurer la boite mail dans un sous-dossier");
		sub.kind = kind;
		sub.requiredTag = "mail/imap";

		return Arrays.asList(replace, sub);
	}

}
