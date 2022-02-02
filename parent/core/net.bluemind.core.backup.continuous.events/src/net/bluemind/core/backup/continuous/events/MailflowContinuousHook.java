package net.bluemind.core.backup.continuous.events;

import net.bluemind.mailflow.api.MailRuleActionAssignmentDescriptor;
import net.bluemind.mailflow.hook.IMailflowHook;

public class MailflowContinuousHook
		implements IMailflowHook, ContinuousContenairization<MailRuleActionAssignmentDescriptor> {

	@Override
	public String type() {
		return "mailflow";
	}

	@Override
	public void onCreate(String domainUid, String uid, MailRuleActionAssignmentDescriptor assignment) {
		save(domainUid, domainUid, uid, assignment, true);
	}

	@Override
	public void onUpdate(String domainUid, String uid, MailRuleActionAssignmentDescriptor assignment) {
		save(domainUid, domainUid, uid, assignment, false);
	}

	@Override
	public void onDelete(String domainUid, String uid, MailRuleActionAssignmentDescriptor assignment) {
		delete(domainUid, domainUid, uid, assignment);
	}
}
