import { MailboxesClient } from "@bluemind/mailbox.api";

let lock = Promise.resolve();
export class MailboxesClientProxy extends MailboxesClient {
    setMailboxFilter() {
        lock = lock.catch(() => {}).then(() => super.setMailboxFilter(...arguments));
        return lock;
    }
}
