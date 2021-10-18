import { UserSettingsClient } from "@bluemind/user.api";

let lock = Promise.resolve();
export class UserSettingsClientProxy extends UserSettingsClient {
    setOne() {
        lock = lock.finally(() => super.setOne(...arguments));
    }
}
