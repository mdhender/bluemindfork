package net.bluemind.role.hook;

import java.util.List;

public class RoleHooks {
	private static List<IRoleHook> hooks = RoleHookActivator.getHooks();

	public static List<IRoleHook> get() {
		return hooks;
	}
}
