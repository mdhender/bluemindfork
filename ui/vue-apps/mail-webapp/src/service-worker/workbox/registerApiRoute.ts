import { registerRoute } from "workbox-routing";
import { RouteHandlerCallback, RouteHandlerCallbackOptions } from "workbox-core/types";
import sortedIndexBy from "lodash.sortedindexby";
import { HTTPMethod } from "workbox-routing/utils/constants";
import pRetry from "p-retry";
import { Field, FilteredChangeSet, Flags, MailItemLight, SortDescriptor } from "../entry";
import { logger } from "../logger";
import Session from "../session";
import { syncMailFolder } from "../sync";

export const apiRoutes = [
    {
        capture: /\/api\/containers\/_subscriptions\/(.+)\/(.+)\/_list/,
        handler: listSubscriptions
    },
    // {
    //     capture: /\/api\/mail_items\/([a-f0-9-]+)\/_mgetById/,
    //     handler: multipleGetById
    // },
    // {
    //     capture: /\/api\/mail_items\/([a-f0-9-]+)\/_filteredChangesetById\?since=0/,
    //     handler: filteredChangesetById
    // },
    // {
    //     capture: /\/api\/mail_items\/([a-f0-9-]+)\/_count/,
    //     handler: count
    // },
    {
        capture: /\/api\/mail_folders\/(.+)\/(.+)\/_all/,
        handler: allMailFolders
    }
];

const methods: HTTPMethod[] = ["GET", "POST", "PUT", "DELETE"];

export default function (routes: { capture: RegExp; handler: RouteHandlerCallback }[]) {
    for (const { capture, handler } of routes) {
        for (const method of methods) {
            registerRoute(capture, handler, method);
        }
    }
}

export async function allMailFolders({ request, params }: RouteHandlerCallbackOptions) {
    if (!(params instanceof Array)) {
        return;
    }
    const [domain, userId] = params;
    try {
        const uid = `${userId}@${domain}`;
        return await retry(async () => {
            const session = await Session.instance();
            if (await session.db.isSubscribed(uid)) {
                const allMailFolders = await session.db.getAllMailFolders(userId);
                return responseFromCache(allMailFolders);
            }
            return fetch(request);
        });
    } catch (error) {
        console.debug(error);
        return fetch(request);
    }
}

export async function multipleGetById({ request, params }: RouteHandlerCallbackOptions) {
    if (params instanceof Array) {
        const [folderUid] = params;
        try {
            request = request as Request;
            const clonedRequest = request.clone();
            const ids = (await clonedRequest.json()) as number[];
            const session = await Session.instance();
            if (await session.db.isSubscribed(folderUid)) {
                const syncOptions = await session.db.getSyncOptions(folderUid);
                if (!!syncOptions?.pending) {
                    await syncMailFolder(folderUid);
                }
                const mailItems = await session.db.getMailItems(folderUid, ids);
                const data = mailItems.filter(Boolean);
                return responseFromCache(data);
            }
            return fetch(request);
        } catch (error) {
            console.debug(error);
            return fetch(request);
        }
    }
}

export async function sortedIds({ request, params }: RouteHandlerCallbackOptions) {
    if (params instanceof Array) {
        const [folderUid] = params;
        try {
            request = request as Request;
            const clonedRequest = request.clone();
            const sortDescriptor = (await clonedRequest.json()) as SortDescriptor;
            const session = await Session.instance();
            if (await session.db.isSubscribed(folderUid)) {
                const syncOptions = await session.db.getSyncOptions(folderUid);
                if (!!syncOptions?.pending) {
                    await syncMailFolder(folderUid);
                }
                const allMailItems: Array<MailItemLight> = await session.db.getAllMailItemLight(folderUid);
                const iteratee = getIteratee(sortDescriptor.fields[0]);
                const data: Array<MailItemLight> = [];
                const ids: Array<number> = [];
                allMailItems.forEach(item => {
                    if (filterByFlags(sortDescriptor.filter, item.flags)) {
                        const index = indexOf(data, item, iteratee, sortDescriptor.fields[0].dir);
                        data.splice(index, 0, item);
                        ids.splice(index, 0, item.internalId);
                    }
                });
                return responseFromCache(ids);
            }
            return fetch(request);
        } catch (error) {
            console.debug(error);
            return fetch(request);
        }
    }
}

export async function filteredChangesetById({ request, params }: RouteHandlerCallbackOptions) {
    if (!(params instanceof Array)) {
        return;
    }
    const [folderUid] = params;
    try {
        request = request as Request;
        const expectedFlags = (await request.clone().json()) as Flags;
        const session = await Session.instance();
        if (await session.db.isSubscribed(folderUid)) {
            const syncOptions = await session.db.getSyncOptions(folderUid);
            if (!!syncOptions?.pending) {
                await syncMailFolder(folderUid);
            }
            const allMailItems: Array<MailItemLight> = await session.db.getAllMailItemLight(folderUid);
            const data: FilteredChangeSet = {
                created: allMailItems
                    .filter(item => filterByFlags(expectedFlags, item.flags))
                    .sort(sortMessageByDate)
                    .map(({ internalId: id }) => ({ id, version: 0 })),
                deleted: [],
                updated: [],
                version: 0
            };
            return responseFromCache(data);
        }
        return fetch(request);
    } catch (error) {
        console.debug(error);
        return fetch(request);
    }
}

export async function count({ request, params }: RouteHandlerCallbackOptions) {
    if (!(params instanceof Array)) {
        return;
    }
    const [folderUid] = params;
    try {
        request = request as Request;
        const expectedFlags = (await request.clone().json()) as Flags;
        const db = await Session.db();
        if (await db.isSubscribed(folderUid)) {
<<<<<<< HEAD
            const allMailItems: Array<MailItemLight> = await db.getAllMailItemLight(folderUid);
=======
            const allMailItems = await db.getAllMailItemLight(folderUid);
>>>>>>> f1841895028 (FEATWEBML-2106 Feat: Add a client proxy plugin mechanism)
            const total = allMailItems.filter(item => filterByFlags(expectedFlags, item.flags)).length;
            return responseFromCache({ total });
        }
        return fetch(request);
    } catch (error) {
        console.debug(error);
        return fetch(request);
    }
}

export async function listSubscriptions({ request, params }: RouteHandlerCallbackOptions) {
    if (!(params instanceof Array)) {
        return;
    }
    const [domain, userId] = params;
    try {
        const db = await Session.db();
        if (await db.isSubscribed(`${userId}@${domain}.subscriptions`)) {
            const mailboxes = await db.getAllOwnerSubscriptions();
            return responseFromCache(mailboxes);
        }
        return fetch(request);
    } catch (error) {
        console.debug(error);
        return fetch(request);
    }
}

function responseFromCache(data: unknown) {
    const headers = new Headers();
    headers.append("X-BM-Fromcache", "true");
    return Promise.resolve(new Response(JSON.stringify(data), { headers }));
}

export function filterByFlags(expectedFlags: Flags, flags: any[]) {
    return (
        expectedFlags.must.every(flag => flags.includes(flag)) &&
        !expectedFlags.mustNot.some(flag => flags.includes(flag))
    );
}

export function getIteratee(field: Field) {
    switch (field.column) {
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
    direction: "Desc" | "Asc"
): number {
    const index = sortedIndexBy(array, value, iteratee);
    if (direction === "Desc") {
        return array.length - index;
    } else {
        return index;
    }
}

export function sortMessageByDate(item1: { date: number }, item2: { date: number }) {
    return item2.date - item1.date;
}

async function retry<T>(fn: () => Promise<T>): Promise<T> {
    const wrapToThrowErrorOnFailure = <T>(fnToWrap: () => Promise<T>): (() => Promise<T>) => {
        return () =>
            fnToWrap().catch((error: any) => {
                logger.log("catching an error", error);
                throw new Error(error);
            });
    };
    return pRetry(wrapToThrowErrorOnFailure(fn), { retries: 1, onFailedAttempt: () => Session.clear() });
}
