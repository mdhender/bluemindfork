import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import { ItemFlag } from "@bluemind/core.container.api";

export default class extends MailboxItemsClient {
    sortedIds(sorted = { column: "internal_date", dir: "Desc" }) {
        if (sorted.column != "internal_date") {
            return super.sortedIds(sorted);
        } else {
            return this.filteredChangesetById(0, { must: [], mustNot: [ItemFlag.Deleted] }).then(changeset => {
                const ids = changeset.created.map(itemVersion => itemVersion.id);
                if (sorted.dir.toLowerCase() == "asc") {
                    return ids.reverse();
                }
                return ids;
            });
        }
    }
}
