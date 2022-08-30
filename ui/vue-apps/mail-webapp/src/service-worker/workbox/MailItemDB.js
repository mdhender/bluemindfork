import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import Session from "../session";
import { syncMailFolder } from "../sync";
import { getMetadatas } from "../getMetadatas";

export class MailItemDB extends MailboxItemsClient {
    async count(filter) {
        try {
            const db = await Session.db();
            if (await db.isSubscribed(this.replicatedMailboxUid)) {
                const allMailItems = await db.getAllMailItemLight(this.replicatedMailboxUid);
                const total = allMailItems.filter(item => filterByFlags(filter, item.flags)).length;
                return { total };
            }
        } catch (error) {
            console.debug(error);
        }
        return this.next();
    }
    async filteredChangesetById(since, filter) {
        try {
            const db = await Session.db();
            if (await db.isSubscribed(this.replicatedMailboxUid)) {
                const syncOptions = await db.getSyncOptions(this.replicatedMailboxUid);
                if (syncOptions?.pending) {
                    await syncMailFolder(this.replicatedMailboxUid);
                }
                const allMailItems = await db.getAllMailItemLight(this.replicatedMailboxUid);
                const changeset /*: FilteredChangeSet*/ = {
                    created: allMailItems
                        .filter(item => filterByFlags(filter, item.flags))
                        .sort(sortMessageByDate)
                        .map(({ internalId: id }) => ({ id, version: 0 })),
                    deleted: [],
                    updated: [],
                    version: 0
                };
                return changeset;
            }
        } catch (error) {
            console.debug(error);
        }
        return this.next();
    }

    async multipleGetById(ids) {
        try {
            const db = await Session.db();
            if (await db.isSubscribed(this.replicatedMailboxUid)) {
                const syncOptions = await db.getSyncOptions(this.replicatedMailboxUid);
                if (syncOptions?.pending) {
                    await syncMailFolder(this.replicatedMailboxUid);
                }
                const mailItems = await db.getMailItems(this.replicatedMailboxUid, ids);
                return mailItems.filter(Boolean);
            }
        } catch (error) {
            console.debug(error);
        }
        return this.next();
    }

    getMetadatas() {
        return getMetadatas();
    }
}

function filterByFlags(filter /*:ItemFlagFilter*/, flags /*:Array<MailboxItemFlag>*/) {
    return filter.must.every(flag => flags.includes(flag)) && !filter.mustNot.some(flag => flags.includes(flag));
}

function sortMessageByDate(item1 /*: { date: number }*/, item2 /*: { date: number }*/) {
    return item2.date - item1.date;
}
