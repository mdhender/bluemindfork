import { registerRoute } from "workbox-routing";
import { RouteHandlerCallbackOptions } from "workbox-core/types";
import { MailDB } from "../MailDB";
import { MailAPI } from "../MailAPI";
import { sync } from "../periodicSync";
import { logger } from "../logger";
import { MailItem } from "../entry";

interface Flags {
    must: string[];
    mustNot: string[];
}

const db = new MailDB();

const apiRoutes = [
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
    }
];

const methods: ("GET" | "POST" | "PUT" | "DELETE")[] = ["GET", "POST", "PUT", "DELETE"];

export default function () {
    for (const { capture, handler } of apiRoutes) {
        for (const method of methods) {
            registerRoute(capture, handler, method);
        }
    }
}

async function multipleById({ request, params: [folderUid] }: RouteHandlerCallbackOptions) {
    try {
        if (request instanceof Request) {
            const clonedRequest = request.clone();
            const { headers } = clonedRequest;
            const sid = headers.get("x-bm-apikey");
            const ids = (await clonedRequest.json()) as number[];
            if ((await db.isSubscribed(folderUid)) && sid !== null) {
                await sync(new MailAPI({ sid }), folderUid);
                return await fetchIndexedDB.multipleById(folderUid, ids);
            }
        }
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, folderUid });
        return fetch(request);
    }
}

async function filteredChangesetById({ request, params: [folderUid] }: RouteHandlerCallbackOptions) {
    try {
        if (request instanceof Request) {
            const expectedFlags = (await request.clone().json()) as Flags;
            const { headers } = await request.clone();
            const sid = headers.get("x-bm-apikey");
            if ((await db.isSubscribed(folderUid)) && sid !== null) {
                await sync(new MailAPI({ sid }), folderUid);
                return fetchIndexedDB.filteredChangesetById(expectedFlags, folderUid);
            }
        }
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, folderUid });
        return fetch(request);
    }
}

async function unreadItems({ request, params: [folderUid] }: RouteHandlerCallbackOptions) {
    try {
        if (request instanceof Request) {
            const { headers } = await request.clone();
            const sid = headers.get("x-bm-apikey");
            const expectedFlags: Flags = { must: [], mustNot: ["Deleted", "Seen"] };
            if ((await db.isSubscribed(folderUid)) && sid !== null) {
                await sync(new MailAPI({ sid }), folderUid);
                return await fetchIndexedDB.unreadItems(folderUid, expectedFlags);
            }
        }
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, folderUid });
        return fetch(request);
    }
}

const fetchIndexedDB = {
    async filteredChangesetById(expectedFlags: Flags, folderUid: string) {
        const allMailItems = await db.getAllMailItems(folderUid);
        const data = {
            created: allMailItems
                .filter(item => filterByFlags(expectedFlags, item))
                .sort(sortMessageByDate)
                .map(({ internalId: id, version }) => ({ id, version })),
            delete: [],
            updated: [],
            version: 0
        };
        const headers = new Headers();
        headers.append("X-BM-Fromcache", "true");
        return new Response(JSON.stringify(data), { status: 200, headers });
    },

    async multipleById(folderUid: string, ids: number[]) {
        const mailItems = await db.getMailItems(folderUid, ids);
        const actualMailItems = mailItems.filter(Boolean);
        if (mailItems.length !== actualMailItems.length) {
            logger.error("Missing some mails in DB");
        }
        const headers = new Headers();
        headers.append("X-BM-Fromcache", "true");
        return new Response(JSON.stringify(actualMailItems), { headers });
    },

    async unreadItems(folderUid: string, expectedFlags: Flags) {
        const allMailItems = await db.getAllMailItems(folderUid);
        const data = allMailItems
            .filter(item => filterByFlags(expectedFlags, item))
            .sort(sortMessageByDate)
            .map(item => item.internalId);
        const headers = new Headers();
        headers.append("X-BM-Fromcache", "true");
        return new Response(JSON.stringify(data), { headers });
    }
};

function filterByFlags(expectedFlags: Flags, item: MailItem) {
    return (
        expectedFlags.must.every(flag => item.flags.includes(flag)) &&
        !expectedFlags.mustNot.some(flag => item.flags.includes(flag))
    );
}

function sortMessageByDate(item1: MailItem, item2: MailItem) {
    return item2.value.body.date - item1.value.body.date;
}
