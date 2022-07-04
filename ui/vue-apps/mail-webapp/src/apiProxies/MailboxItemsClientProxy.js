import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import { ItemFlag } from "@bluemind/core.container.api";
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

    fetch() {
        lock = lock.catch(() => {}).then(() => super.fetch(...arguments));
        return lock;
    }

    getForUpdate() {
        lock = lock.catch(() => {}).then(() => super.getForUpdate(...arguments));
        return lock;
    }

    sortedIds(sorted = { column: "internal_date", dir: "Desc" }) {
        if (sorted.column !== "internal_date") {
            return super.sortedIds(sorted);
        } else {
            return this.filteredChangesetById(0, { must: [], mustNot: [ItemFlag.Deleted] }).then(changeset => {
                const ids = changeset.created.map(itemVersion => itemVersion.id);
                if (sorted.dir.toLowerCase() === "asc") {
                    return ids.reverse();
                }
                return ids;
            });
        }
    }
}
