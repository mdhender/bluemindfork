import { registerRoute } from "workbox-routing";
import { MailDB } from "../MailDB";
import { MailAPI } from "../MailAPI";
import { sync } from "../periodicSync";

const db = new MailDB();

const apiRoutes = [
    {
        capture: /\/api\/mail_items\/([a-f0-9-]+)\/_multipleById/,
        handler: multipleByIdHandler
    },
    {
        capture: /\/api\/mail_items\/([a-f0-9-]+)\/_filteredChangesetById\?since=0/,
        handler: filteredChangesetByIdHandler
    },
    {
        capture: /\/api\/mail_items\/([a-f0-9-]+)\/_unread/,
        handler: unreadItemsHandler
    }
];

function prehandler(handler) {
    return async options => {
        const {
            request: { headers },
            params: [folderUid]
        } = options;
        const sid = headers.get("x-bm-apikey");
        await sync(new MailAPI(sid), folderUid);
        return handler(options);
    };
}

export default function () {
    for (const { capture, handler } of apiRoutes) {
        for (const method of ["GET", "POST", "PUT", "DELETE"]) {
            registerRoute(capture, prehandler(handler), method);
        }
    }
}

async function multipleByIdHandler({ request, params: [folderUid] }) {
    if (await db.isInFolderSyncInfo(folderUid)) {
        const ids = await request.clone().json();
        if (Array.isArray(ids) && ids.length > 0) {
            const { localMailItems, localIds, remoteIds } = await db.getMixedMailItems(folderUid, ids);
            const response = await fetch(new Request(request.clone(), { body: JSON.stringify(remoteIds) }));
            const mailItems = await response.json();
            await db.putMailItems(mailItems, folderUid);
            const data = localMailItems.concat(mailItems);
            const headers = new Headers(response.headers);
            headers.append("X-BM-Fromcache", true);
            headers.append("X-BM-LocalIds", JSON.stringify(localIds));
            headers.append("X-BM-RemoteIds", JSON.stringify(remoteIds));
            return new Response(JSON.stringify(data), { headers });
        }
    }
    return await fetch(request);
}

async function filteredChangesetByIdHandler({ request, params: [folderUid] }) {
    const expectedFlags = await request.clone().json();
    const syncUpToDate = await db.isFullySynced(folderUid);
    try {
        if (syncUpToDate) {
            return fromIndexedDB(expectedFlags, folderUid);
        } else {
            return fetch(request);
        }
    } catch (error) {
        return fromIndexedDB(expectedFlags, folderUid);
    }
}

async function fromIndexedDB(expectedFlags, folderUid) {
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
}

async function unreadItemsHandler({ request, params: [folderUid] }) {
    if (await db.isFullySynced(folderUid)) {
        const expectedFlags = { must: [], mustNot: ["Deleted", "Seen"] };
        const allMailItems = await db.getAllMailItems(folderUid);
        const data = allMailItems
            .filter(item => filterByFlags(expectedFlags, item))
            .sort(sortMessageByDate)
            .map(item => item.internalId);

        const headers = new Headers();
        headers.append("X-BM-Fromcache", true);
        return new Response(JSON.stringify(data), { headers });
    }
    return await fetch(request);
}
function filterByFlags(expectedFlags, item) {
    return (
        expectedFlags.must.every(flag => item.flags.includes(flag)) &&
        !expectedFlags.mustNot.some(flag => item.flags.includes(flag))
    );
}

function sortMessageByDate(item1, item2) {
    return item2.value.body.date - item1.value.body.date;
}
