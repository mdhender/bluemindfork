import { MailConversationActionsClient } from "@bluemind/backend.mail.api";
import imapLock from "@bluemind/imap-lock";

let lock = imapLock;

export default class extends MailConversationActionsClient {
    importItems() {
        lock = lock.catch(() => {}).then(() => super.importItems(...arguments));
        return lock;
    }

    move() {
        lock = lock.catch(() => {}).then(() => super.move(...arguments));
        return lock;
    }

    addFlag() {
        lock = lock.catch(() => {}).then(() => super.addFlag(...arguments));
        return lock;
    }

    deleteFlag() {
        lock = lock.catch(() => {}).then(() => super.deleteFlag(...arguments));
        return lock;
    }

    copy() {
        lock = lock.catch(() => {}).then(() => super.copy(...arguments));
        return lock;
    }
}
