import { MailboxItemsClient, MailboxFoldersClient } from "@bluemind/backend.mail.api";
import { ItemFlag } from "@bluemind/core.container.api";

let sequentialRequest = Promise.resolve();

export class MailboxItemsClientProxy extends MailboxItemsClient {
    addFlag(...args) {
        sequentialRequest = sequentialRequest.then(() => super.addFlag(...args)).catch(() => super.addFlag(...args));
        return sequentialRequest;
    }

    deleteFlag(...args) {
        sequentialRequest = sequentialRequest
            .then(() => super.deleteFlag(...args))
            .catch(() => super.deleteFlag(...args));
        return sequentialRequest;
    }

    fetch(...args) {
        sequentialRequest = sequentialRequest.then(() => super.fetch(...args)).catch(() => super.fetch(...args));
        return sequentialRequest;
    }

    getForUpdate(...args) {
        sequentialRequest = sequentialRequest
            .then(() => super.getForUpdate(...args))
            .catch(() => super.getForUpdate(...args));
        return sequentialRequest;
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

export class MailboxFoldersClientProxy extends MailboxFoldersClient {
    importItems(...args) {
        sequentialRequest = sequentialRequest
            .then(() => super.importItems(...args))
            .catch(() => super.importItems(...args));
        return sequentialRequest;
    }
}
