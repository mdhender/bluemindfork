import { registerRoute } from "workbox-routing";
import { RouteHandlerCallback, RouteHandlerCallbackOptions } from "workbox-core/types";
import { syncMailFolder } from "../periodicSync";
import { maildb } from "../MailDB";
import { FilteredChangeSet, Flags } from "../entry";
import { getDBName } from "../MailAPI";
import { HTTPMethod } from "workbox-routing/utils/constants";

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
        capture: /\/api\/mail_items\/([a-f0-9-]+)\/_unread/,
        handler: unreadItems
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
            const db = await maildb.getInstance(await getDBName());
            if (await db.isSubscribed(uid)) {
                const allMailFolders = await db.getAllMailFolders();
                return responseFromCache(allMailFolders);
            }
            return fetch(request);
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
            const db = await maildb.getInstance(await getDBName());
            if (await db.isSubscribed(folderUid)) {
                await syncMailFolder(folderUid);
                const mailItems = await db.getMailItems(folderUid, ids);
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
            const db = await maildb.getInstance(await getDBName());
            if (await db.isSubscribed(folderUid)) {
                await syncMailFolder(folderUid);
                const allMailItems = await db.getAllMailItemLight(folderUid);
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

export async function unreadItems({ request, params }: RouteHandlerCallbackOptions) {
    if (params instanceof Array) {
        const [folderUid] = params;
        try {
            request = request as Request;
            const expectedFlags: Flags = { must: [], mustNot: ["Deleted", "Seen"] };
            const db = await maildb.getInstance(await getDBName());
            if (await db.isSubscribed(folderUid)) {
                await syncMailFolder(folderUid);
                const allMailItems = await db.getAllMailItems(folderUid);
                const data = allMailItems
                    .filter(item => filterByFlags(expectedFlags, item.flags))
                    .sort((item1, item2) => sortMessageByDate(item1.value.body, item2.value.body))
                    .map(item => item.internalId);
                return responseFromCache(data);
            }
            return fetch(request);
        } catch (error) {
            console.debug(error);
            return fetch(request);
        }
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
