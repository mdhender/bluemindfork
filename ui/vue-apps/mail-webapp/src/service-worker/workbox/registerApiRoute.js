import { registerRoute } from "workbox-routing";
import { MailDB } from "../MailDB";

const db = new MailDB();

export default function () {
    registerApiRoute(MAILITEMS_MULTIPLEBYID);
    registerApiRoute(MAILITEMS_CHANGESETSINCE0);
    registerApiRoute(MAILITEMS_UNREAD);
}

function registerApiRoute(index) {
    for (const method of ["GET", "POST", "PUT", "DELETE"]) {
        registerRoute(apiRoutes[index].capture, apiRoutes[index].handler, method);
    }
}

const MAILITEMS_MULTIPLEBYID = "mail_items/:uid/_multipleById";
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

const MAILITEMS_CHANGESETSINCE0 = "mail_items/:uid/_filteredChangesetById?since=0";
async function filteredChangesetByIdHandler({ request, params: [folderUid] }) {
    if (await db.isInFolderSyncInfo(folderUid)) {
        const expectedFlags = await request.json();
        const allMailItems = await new MailDB().getAllMailItems(folderUid);
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
        return new Response(JSON.stringify(data), { headers });
    }
    return await fetch(request);
}

const MAILITEMS_UNREAD = "mail_items/:uid/_unread";
async function unreadItemsHandler({ request, params: [folderUid] }) {
    if (await db.isInFolderSyncInfo(folderUid)) {
        const allMailItems = await db.getAllMailItems(folderUid);
        const expectedFlags = { must: [], mustNot: ["Deleted", "Seen"] };
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

const apiRoutes = {
    [MAILITEMS_MULTIPLEBYID]: {
        capture: /\/api\/mail_items\/([a-f0-9-]+)\/_multipleById/,
        handler: multipleByIdHandler
    },
    [MAILITEMS_CHANGESETSINCE0]: {
        capture: /\/api\/mail_items\/([a-f0-9-]+)\/_filteredChangesetById\?since=0/,
        handler: filteredChangesetByIdHandler
    },
    [MAILITEMS_UNREAD]: {
        capture: /\/api\/mail_items\/([a-f0-9-]+)\/_unread/,
        handler: unreadItemsHandler
    }
};
