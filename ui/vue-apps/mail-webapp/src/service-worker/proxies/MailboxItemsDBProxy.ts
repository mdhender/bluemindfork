/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { sortBy } from "lodash";
import { MailboxItemsClient } from "@bluemind/backend.mail.api";
import { ItemFlag, ItemFlagFilter, SortDescriptor } from "@bluemind/core.container.api";
import { isSubscribedAndSynced } from "../sync";
import { default as db, MailItemLight } from "../MailDB";

export default class extends MailboxItemsClient {
    next?: (...args: Array<unknown>) => Promise<never>;

    async count(filter: ItemFlagFilter) {
        try {
            if (await isSubscribedAndSynced(this.replicatedMailboxUid)) {
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
            if (await isSubscribedAndSynced(this.replicatedMailboxUid)) {
                const mailItems = await db.getMailItems(this.replicatedMailboxUid, ids);
                return mailItems.filter(NotNull);
            }
        } catch (error) {
            console.debug(error);
        }
        return this.next!();
    }

    async sortedIds(sort?: SortDescriptor) {
        if (await isSubscribedAndSynced(this.replicatedMailboxUid)) {
            const allMailItems: Array<MailItemLight> = await db.getAllMailItemLight(this.replicatedMailboxUid);
            const iteratee = getIteratee(sort?.fields?.at(0));
            const sorted = sortBy(allMailItems, iteratee);
            const ids: number[] = [];
            sorted.forEach(({ internalId, flags }) => {
                if (matchFilter(flags, sort?.filter)) {
                    ids.push(internalId);
                }
            });
            return sort?.fields?.at(0)?.dir === "Desc" ? ids.reverse() : ids;
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

export function filterByFlags(expected: ItemFlagFilter | undefined, flags: ItemFlag[]) {
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
