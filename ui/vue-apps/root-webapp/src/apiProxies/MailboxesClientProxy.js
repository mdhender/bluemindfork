import { MailboxesClient } from "@bluemind/mailbox.api";

// BM core does not support parallel execution of this request
let lock = Promise.resolve();

export default class extends MailboxesClient {
    setMailboxFilter() {
        lock = lock.catch(() => {}).then(() => super.setMailboxFilter(...arguments));
        return lock;
    }
}
