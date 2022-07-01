package net.bluemind.user.hook.settings;

public interface IUserSettingsHook {

	void onSettingsUpdate(String containerUid, String userUid);

}
