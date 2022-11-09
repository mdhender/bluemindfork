import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import imapLock from "@bluemind/imap-lock";

let lock = imapLock;

export default class extends MailboxItemsClient {
    addFlag() {
        lock = lock.catch(() => {}).then(() => super.addFlag(...arguments));
        return lock;
    }

    deleteFlag() {
        lock = lock.catch(() => {}).then(() => super.deleteFlag(...arguments));
        return lock;
    }

    deleteById() {
        lock = lock.catch(() => {}).then(() => super.deleteById(...arguments));
        return lock;
    }

    fetch() {
        lock = lock.catch(() => {}).then(() => super.fetch(...arguments));
        return lock;
    }

    getForUpdate() {
        lock = lock.catch(() => {}).then(() => super.getForUpdate(...arguments));
        return lock;
    }

    multipleDeleteById() {
        lock = lock.catch(() => {}).then(() => super.multipleDeleteById(...arguments));
        return lock;
    }
}
