import { registerRoute } from "workbox-routing";
import { RouteHandlerCallbackOptions } from "workbox-core/types";
import { syncMailFolder } from "../periodicSync";
import { maildb } from "../MailDB";
import { logger } from "../logger";
import { MailItem } from "../entry";

interface Flags {
    must: string[];
    mustNot: string[];
}

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
        const uid = `${userId}@${domain}`;
        if (await (await maildb.getInstance()).isSubscribed(uid)) {
            return await fetchIndexedDB.allMailFolders();
        }
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, domain, userId });
        return fetch(request);
    }
}

async function multipleById({ request, params: [folderUid] }: RouteHandlerCallbackOptions) {
    try {
        request = request as Request;
        const clonedRequest = request.clone();
        const ids = (await clonedRequest.json()) as number[];
        if (await (await maildb.getInstance()).isSubscribed(folderUid)) {
            try {
                await syncMailFolder(folderUid);
            } catch (err) {
                logger.error("Impossible to reach the server: using local data, they may be outdated.");
            }
            return await fetchIndexedDB.multipleById(folderUid, ids);
        }
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, folderUid });
        return fetch(request);
    }
}

async function filteredChangesetById({ request, params: [folderUid] }: RouteHandlerCallbackOptions) {
    try {
        request = request as Request;
        const expectedFlags = (await request.clone().json()) as Flags;
        if (await (await maildb.getInstance()).isSubscribed(folderUid)) {
            try {
                await syncMailFolder(folderUid);
            } catch (err) {
                logger.error("Impossible to reach the server: using local data, they may be outdated.");
            }
            return fetchIndexedDB.filteredChangesetById(expectedFlags, folderUid);
        }
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, folderUid });
        return fetch(request);
    }
}

async function unreadItems({ request, params: [folderUid] }: RouteHandlerCallbackOptions) {
    try {
        request = request as Request;
        const expectedFlags: Flags = { must: [], mustNot: ["Deleted", "Seen"] };
        if (await (await maildb.getInstance()).isSubscribed(folderUid)) {
            try {
                await syncMailFolder(folderUid);
            } catch (err) {
                logger.error("Impossible to reach the server: using local data, they may be outdated.");
            }
            return await fetchIndexedDB.unreadItems(folderUid, expectedFlags);
        }
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, folderUid });
        return fetch(request);
    }
}

const fetchIndexedDB = {
    async filteredChangesetById(expectedFlags: Flags, folderUid: string) {
        const allMailItems = await (await maildb.getInstance()).getAllMailItemLight(folderUid);
        const data = {
            created: allMailItems
                .filter(item => filterByFlags(expectedFlags, item.flags))
                .sort((i1, i2) => i2.date - i1.date)
                .map(({ internalId: id }) => ({ id })),
            delete: [],
            updated: [],
            version: 0
        };
        const headers = new Headers();
        headers.append("X-BM-Fromcache", "true");
        return new Response(JSON.stringify(data), { status: 200, headers });
    },

    async multipleById(folderUid: string, ids: number[]) {
        const mailItems = await (await maildb.getInstance()).getMailItems(folderUid, ids);
        const actualMailItems = mailItems.filter(Boolean);
        if (mailItems.length !== actualMailItems.length) {
            logger.error("Missing some mails in DB");
        }
        const headers = new Headers();
        headers.append("X-BM-Fromcache", "true");
        return new Response(JSON.stringify(actualMailItems), { headers });
    },

    async unreadItems(folderUid: string, expectedFlags: Flags) {
        const allMailItems = await (await maildb.getInstance()).getAllMailItems(folderUid);
        const data = allMailItems
            .filter(item => filterByFlags(expectedFlags, item.flags))
            .sort(sortMessageByDate)
            .map(item => item.internalId);
        const headers = new Headers();
        headers.append("X-BM-Fromcache", "true");
        return new Response(JSON.stringify(data), { headers });
    },
    async allMailFolders() {
        const folders = await (await maildb.getInstance()).getAllMailFolders();
        const headers = new Headers();
        headers.append("X-BM-Fromcache", "true");
        return new Response(JSON.stringify(folders), { headers });
    }
};

function filterByFlags(expectedFlags: Flags, flags: any[]) {
    return (
        expectedFlags.must.every(flag => flags.includes(flag)) &&
        !expectedFlags.mustNot.some(flag => flags.includes(flag))
    );
}

function sortMessageByDate(item1: MailItem, item2: MailItem) {
    return item2.value.body.date - item1.value.body.date;
}
