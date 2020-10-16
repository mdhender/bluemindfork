import { registerRoute } from "workbox-routing";
import { MailDB } from "../MailDB";
import { MailAPI } from "../MailAPI";
import { sync } from "../periodicSync";
import { logger } from "../logger";

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

export default function () {
    for (const { capture, handler } of apiRoutes) {
        for (const method of ["GET", "POST", "PUT", "DELETE"]) {
            registerRoute(capture, handler, method);
        }
    }
}

async function multipleById({ request, params: [folderUid] }) {
    try {
        const ids = await request.clone().json();
        if (await db.isSubscribed(folderUid)) {
            return await fetchIndexedDB.multipleById(folderUid, ids);
        }
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, folderUid });
        return fetch(request);
    }
}

async function filteredChangesetById({ request, params: [folderUid] }) {
    try {
        const expectedFlags = await request.clone().json();
        const { headers } = await request.clone();
        const sid = headers.get("x-bm-apikey");
        if (await db.isSubscribed(folderUid)) {
            await sync(new MailAPI({ sid }), folderUid);
            return fetchIndexedDB.filteredChangesetById(expectedFlags, folderUid);
        }
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, folderUid });
        return fetch(request);
    }
}

async function unreadItems({ request, params: [folderUid] }) {
    try {
        const { headers } = await request.clone();
        const sid = headers.get("x-bm-apikey");
        const expectedFlags = { must: [], mustNot: ["Deleted", "Seen"] };
        if (await db.isSubscribed(folderUid)) {
            await sync(new MailAPI({ sid }), folderUid);
            return await fetchIndexedDB.unreadItems(folderUid, expectedFlags);
        }
        return fetch(request);
    } catch (error) {
        logger.error("Error with local data", { error, folderUid });
        return fetch(request);
    }
}

const fetchIndexedDB = {
    async filteredChangesetById(expectedFlags, folderUid) {
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
        headers.append("X-BM-Fromcache", true);
        return new Response(JSON.stringify(data), { status: 200, headers });
    },

    async multipleById(folderUid, ids) {
        const mailItems = await db.getAllMailItems(folderUid, ids);
        const headers = new Headers();
        headers.append("X-BM-Fromcache", true);
        return new Response(JSON.stringify(mailItems), { headers });
    },

    async unreadItems(folderUid, expectedFlags) {
        const allMailItems = await db.getAllMailItems(folderUid);
        const data = allMailItems
            .filter(item => filterByFlags(expectedFlags, item))
            .sort(sortMessageByDate)
            .map(item => item.internalId);
        const headers = new Headers();
        headers.append("X-BM-Fromcache", true);
        return new Response(JSON.stringify(data), { headers });
    }
};

function filterByFlags(expectedFlags, item) {
    return (
        expectedFlags.must.every(flag => item.flags.includes(flag)) &&
        !expectedFlags.mustNot.some(flag => item.flags.includes(flag))
    );
}

function sortMessageByDate(item1, item2) {
    return item2.value.body.date - item1.value.body.date;
}
