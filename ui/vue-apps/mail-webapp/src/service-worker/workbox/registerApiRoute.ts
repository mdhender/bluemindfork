import { registerRoute } from "workbox-routing";
import { RouteHandlerCallbackOptions } from "workbox-core/types";
import { syncMailFolder } from "../periodicSync";
import { MailDB } from "../MailDB";
import { logger } from "../logger";
import { MailItem } from "../entry";
import { createFolderId } from "../MailAPI";

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
    },
    {
        capture: /\/api\/mail_folders\/(.+)\/(.+)\/_all/,
        handler: allMailFolders
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

async function allMailFolders({ request, params: [domain, userId] }: RouteHandlerCallbackOptions) {
    try {
        request = request as Request;
        const uid = createFolderId({ userId, domain });
        logger.log("cache or network?");
        if (await db.isSubscribed(uid)) {
            logger.log("cache", { uid });
            return await fetchIndexedDB.allMailFolders();
        }
        logger.log("network", { uid });
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, domain, userId });
        return fetch(request);
    }
}

async function multipleById({ request, params: [folderUid] }: RouteHandlerCallbackOptions) {
    try {
        if (request instanceof Request) {
            const clonedRequest = request.clone();
            const ids = (await clonedRequest.json()) as number[];
            if (await db.isSubscribed(folderUid)) {
                await syncMailFolder(folderUid);
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
            if (await db.isSubscribed(folderUid)) {
                await syncMailFolder(folderUid);
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
            const expectedFlags: Flags = { must: [], mustNot: ["Deleted", "Seen"] };
            if (await db.isSubscribed(folderUid)) {
                await syncMailFolder(folderUid);
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
    },
    async allMailFolders() {
        const folders = await db.getAllMailFolders();
        const headers = new Headers();
        headers.append("X-BM-Fromcache", "true");
        return new Response(JSON.stringify(folders), { headers });
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
