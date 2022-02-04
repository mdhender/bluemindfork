import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import { ItemFlag } from "@bluemind/core.container.api";

let lock = Promise.resolve();

export class MailboxItemsClientProxy extends MailboxItemsClient {
    addFlag() {
        lock = lock.catch(Function).then(() => super.addFlag(...arguments));
        return lock;
    }

    deleteFlag() {
        lock = lock.catch(Function).then(() => super.deleteFlag(...arguments));
        return lock;
    }

    fetch() {
        lock = lock.catch(Function).then(() => super.fetch(...arguments));
        return lock;
    }

    getForUpdate() {
        lock = lock.catch(Function).then(() => super.getForUpdate(...arguments));
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
