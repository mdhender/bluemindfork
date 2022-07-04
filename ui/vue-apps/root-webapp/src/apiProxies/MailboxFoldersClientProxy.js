import { MailboxFoldersClient } from "@bluemind/backend.mail.api";
import imapLock from "@bluemind/imap-lock";

let lock = imapLock;

export default class extends MailboxFoldersClient {
    importItems() {
        lock = lock.catch(() => {}).then(() => super.importItems(...arguments));
        return lock;
    }
}
