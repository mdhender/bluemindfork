package net.bluemind.core.backup.continuous.events;

import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.UserMailIdentity;
import net.bluemind.user.hook.identity.IUserMailIdentityHook;

public class UserMailIdentitiesContinuousHook
		implements IUserMailIdentityHook, ContinuousContenairization<UserMailIdentity> {

	@Override
	public String type() {
		return "userMailIdentities";
	}

	@Override
	public void onIdentityCreated(BmContext context, String domainUid, String userUid, String id,
			UserMailIdentity current) {
		save(domainUid, userUid, id, current, true);
	}

	@Override
	public void onIdentityUpdated(BmContext context, String domainUid, String userUid, String id,
			UserMailIdentity current, UserMailIdentity previous) {
		save(domainUid, userUid, id, current, false);
	}

	@Override
	public void onIdentityDeleted(BmContext context, String domainUid, String userUid, String id,
			UserMailIdentity previous) {
		delete(domainUid, userUid, id, previous);
	}

	@Override
	public void onIdentityDefault(BmContext context, String domainUid, String userUid, String id) {
		// Do nothing
	}

	@Override
	public void beforeCreate(BmContext context, String domainUid, String uid, UserMailIdentity identity) {
		// Do nothing
	}

	@Override
	public void beforeUpdate(BmContext context, String domainUid, String uid, UserMailIdentity update,
			UserMailIdentity previous) {
		// Do nothing
	}

	@Override
	public void beforeDelete(BmContext context, String domainUid, String uid, UserMailIdentity previous) {
		// Do nothing
	}

}
