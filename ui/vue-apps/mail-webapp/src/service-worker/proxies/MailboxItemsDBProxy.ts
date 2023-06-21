/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import { ItemFlag, ItemFlagFilter, SortDescriptor } from "@bluemind/core.container.api";
import Session from "../session";
import { syncMailFolder } from "../sync";
import sortedIndexBy from "lodash.sortedindexby";
import { MailItemLight } from "../MailDB";

export default class extends MailboxItemsClient {
    next?: (...args: Array<unknown>) => Promise<never>;

    async count(filter: ItemFlagFilter) {
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
        return this.next!();
    }

    async multipleGetById(ids: number[]) {
        try {
            const db = await Session.db();
            if (await db.isSubscribed(this.replicatedMailboxUid)) {
                const syncOptions = await db.getSyncOptions(this.replicatedMailboxUid);
                if (syncOptions?.pending) {
                    await syncMailFolder(this.replicatedMailboxUid);
                }
                const mailItems = await db.getMailItems(this.replicatedMailboxUid, ids);
                return mailItems.filter(NotNull);
            }
        } catch (error) {
            console.debug(error);
        }
        return this.next!();
    }

    async sortedIds(sort?: SortDescriptor) {
        const db = await Session.db();
        sort = sort as SortDescriptor;
        if (await db.isSubscribed(this.replicatedMailboxUid)) {
            const syncOptions = await db.getSyncOptions(this.replicatedMailboxUid);
            if (syncOptions?.pending) {
                await syncMailFolder(this.replicatedMailboxUid);
            }
            const allMailItems: Array<MailItemLight> = await db.getAllMailItemLight(this.replicatedMailboxUid);
            const iteratee = getIteratee(sort?.fields?.at(0));
            const data: Array<MailItemLight> = [];
            const ids: Array<number> = [];
            allMailItems.forEach(item => {
                if (matchFilter(item.flags, sort?.filter)) {
                    const index = indexOf(data, item, iteratee, sort?.fields?.at(0)?.dir);
                    data.splice(index, 0, item);
                    ids.splice(index, 0, item.internalId);
                }
            });
            return ids;
        }
        return this.next!();
    }
}

function getIteratee(field?: SortDescriptor.Field) {
    switch (field?.column) {
        case "date":
        case "size":
        case "subject":
        case "sender":
            return field.column;
        case "internal_date":
        default:
            return "date";
    }
}
function indexOf(
    array: Array<MailItemLight>,
    value: MailItemLight,
    iteratee: string,
    direction?: "Desc" | "Asc"
): number {
    const index = sortedIndexBy(array, value, iteratee);
    return direction === "Desc" ? array.length - index : index;
}

export function filterByFlags(expected: ItemFlagFilter | undefined, flags: any[]) {
    return (
        expected?.must?.every(flag => flags.includes(flag)) && !expected?.mustNot?.some(flag => flags.includes(flag))
    );
}
function matchFilter(flags: Array<ItemFlag>, filter?: ItemFlagFilter) {
    return filter?.must?.every(flag => flags.includes(flag)) && !filter.mustNot?.some(flag => flags.includes(flag));
}

function NotNull<T>(value: T): value is NonNullable<T> {
    return value !== null && value !== undefined;
}
