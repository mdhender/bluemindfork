package net.bluemind.core.backup.continuous.events;

import net.bluemind.user.accounts.hook.IUserAccountsHook;
import net.bluemind.user.api.UserAccount;

public class UserAccountsContinuousHook implements IUserAccountsHook, ContinuousContenairization<UserAccount> {

	@Override
	public String type() {
		return "userAccounts";
	}

	@Override
	public void onCreate(String domainUid, String uid, String systemIdentifier, UserAccount account) {
		save(domainUid, uid, systemIdentifier, account, true);
	}

	@Override
	public void onUpdate(String domainUid, String uid, String systemIdentifier, UserAccount account) {
		save(domainUid, uid, systemIdentifier, account, false);
	}

	@Override
	public void onDelete(String domainUid, String uid, String systemIdentifier) {
		delete(domainUid, uid, systemIdentifier, new UserAccount());
	}
}
