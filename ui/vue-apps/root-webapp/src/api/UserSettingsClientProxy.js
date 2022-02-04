import { UserSettingsClient } from "@bluemind/user.api";

let lock = Promise.resolve();
export class UserSettingsClientProxy extends UserSettingsClient {
    setOne() {
        lock = lock.catch(Function).then(() => super.setOne(...arguments));
        return lock;
    }
}
