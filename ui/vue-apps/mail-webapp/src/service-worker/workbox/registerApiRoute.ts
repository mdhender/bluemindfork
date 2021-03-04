import { registerRoute } from "workbox-routing";
import { RouteHandlerCallback, RouteHandlerCallbackOptions } from "workbox-core/types";
import { syncMailbox } from "../sync";
import { FilteredChangeSet, Flags } from "../entry";
import { HTTPMethod } from "workbox-routing/utils/constants";
import pRetry from "p-retry";
import { logger } from "../logger";
import Session from "../session";

export const apiRoutes = [
    {
        capture: /\/api\/mail_items\/([a-f0-9-]+)\/_multipleById/,
        handler: multipleById
    },
    {
        capture: /\/api\/mail_items\/([a-f0-9-]+)\/_filteredChangesetById\?since=0/,
        handler: filteredChangesetById
    },
    {
        capture: /\/api\/mail_items\/([a-f0-9-]+)\/_count/,
        handler: count
    },
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
    if (params instanceof Array) {
        const [domain, userId] = params;
        try {
            const uid = `${userId}@${domain}`;
            return await retry( async () => {
                const session = await Session.instance();
                if (await session.db.isSubscribed(uid)) {
                    await syncMailbox(domain, userId.replace('user.', ''));
                    const allMailFolders = await session.db.getAllMailFolders();
                    return responseFromCache(allMailFolders);
                }
                return fetch(request);
            });
        } catch (error) {
            console.debug(error);
            return fetch(request);
        }
    }
}

export async function multipleById({ request, params }: RouteHandlerCallbackOptions) {
    if (params instanceof Array) {
        const [folderUid] = params;
        try {
            request = request as Request;
            const clonedRequest = request.clone();
            const ids = (await clonedRequest.json()) as number[];
            const session = await Session.instance();
            if (await session.db.isSubscribed(folderUid)) {
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

export async function filteredChangesetById({ request, params }: RouteHandlerCallbackOptions) {
    if (params instanceof Array) {
        const [folderUid] = params;
        try {
            request = request as Request;
            const expectedFlags = (await request.clone().json()) as Flags;
            const session = await Session.instance();
            if (await session.db.isSubscribed(folderUid)) {
                const allMailItems = await session.db.getAllMailItemLight(folderUid);
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
            const allMailItems = await db.getAllMailItems(folderUid);
            const total = allMailItems
                .filter(item => filterByFlags(expectedFlags, item.flags))
                .length;
            return responseFromCache({ total });
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

export function sortMessageByDate(item1: { date: number }, item2: { date: number }) {
    return item2.date - item1.date;
}

async function retry<T>(fn: () => Promise<T>): Promise<T> {
    const wrapToThrowErrorOnFailure = <T>(fnToWrap: () => Promise<T>): (() => Promise<T>) => {
        return () =>
            fnToWrap().catch((error: any) => {
                logger.log("catching an error", error)
                throw new Error(error);
            });
    };
    return pRetry(wrapToThrowErrorOnFailure(fn), { retries: 1, onFailedAttempt: () => Session.clear() });
}
