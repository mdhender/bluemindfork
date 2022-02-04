import { MailboxFoldersClient } from "@bluemind/backend.mail.api";

let lock = Promise.resolve();
export class MailboxFoldersClientProxy extends MailboxFoldersClient {
    importItems() {
        lock = lock.catch(Function).then(() => super.importItems(...arguments));
        return lock;
    }
}
