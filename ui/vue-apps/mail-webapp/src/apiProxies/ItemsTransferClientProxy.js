import { ItemsTransferClient } from "@bluemind/backend.mail.api";
import imapLock from "@bluemind/imap-lock";

let lock = imapLock;

export default class extends ItemsTransferClient {
    move() {
        lock = lock.catch(() => {}).then(() => super.move(...arguments));
        return lock;
    }

    copy() {
        lock = lock.catch(() => {}).then(() => super.copy(...arguments));
        return lock;
    }
}
