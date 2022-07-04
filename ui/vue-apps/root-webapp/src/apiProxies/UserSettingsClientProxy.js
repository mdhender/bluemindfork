import { UserSettingsClient } from "@bluemind/user.api";

// BM core does not support parallel execution of this request
let lock = Promise.resolve();

export default class extends UserSettingsClient {
    setOne() {
        lock = lock.catch(() => {}).then(() => super.setOne(...arguments));
        return lock;
    }
}
