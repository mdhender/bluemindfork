import { MailIDB } from "../mailIDB";

export default async function ({ request }) {
    const splittedUrl = request.url.split("/");
    const apiUrl = splittedUrl.slice(4, splittedUrl.length).join("/");
    if (isApiSupported(apiUrl)) {
        const apiMethod = await useIndexedDB(request);
        if (apiMethod) {
            return new Response(JSON.stringify(await getResponseBody[apiMethod](request)), {
                headers: {
                    fromcache: "true"
                }
            });
        }
    }
    return fetch(request);
}

function isApiSupported(url) {
    return url.startsWith("mail_items") || url.startsWith("mail_folders");
}

async function useIndexedDB(request) {
    let regex = /\/api\/mail_items\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\/(.*)$/;
    const match = regex.exec(request.url);
    if (match) {
        const db = new MailIDB();
        const [, uid, apiMethod] = match;
        const folder = await db.getSyncedFolder({ uid });
        if (folder && getResponseBody[apiMethod]) {
            return apiMethod;
        }
    }
    return false;
}

const getResponseBody = {
    "_filteredChangesetById?since=0": filteredChangesetById,
    _multipleById: multipleById,
    _unread: unreadItems
};

async function multipleById(request) {
    const ids = JSON.parse(await request.text());
    return await new MailIDB().getMailItems(ids.map(id => ({ internalId: id })));
}

async function unreadItems() {
    const allMailItems = await new MailIDB().getAllMailItems();
    const expectedFlags = { must: [], mustNot: ["Deleted", "Seen"] };
    return allMailItems
        .filter(item => filterByFlags(expectedFlags, item))
        .sort(sortMessage)
        .map(item => item.internalId);
}

async function filteredChangesetById(request) {
    const expectedFlags = await request.json();
    const allMailItems = await new MailIDB().getAllMailItems();
    return {
        created: allMailItems
            .filter(item => filterByFlags(expectedFlags, item))
            .sort(sortMessage)
            .map(({ internalId: id, version }) => ({ id, version })),
        delete: [],
        updated: [],
        version: 0
    };
}

function filterByFlags(expectedFlags, item) {
    return (
        expectedFlags.must.every(flag => item.flags.includes(flag)) &&
        !expectedFlags.mustNot.some(flag => item.flags.includes(flag))
    );
}

function sortMessage(item1, item2) {
    return item2.value.body.date - item1.value.body.date;
}
