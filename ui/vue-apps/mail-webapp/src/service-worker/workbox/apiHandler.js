import { MailIDB } from "../mailIDB";

// let sequentialRequest = Promise.resolve();

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
        // else if (isSequentialRequest(apiUrl, request.method)) {
        //     sequentialRequest = sequentialRequest.then(() => fetch(request)).catch(() => fetch(request));
        //     return sequentialRequest;
        // }
    }
    return fetch(request);
}

// const generateImapRequests = {
//     mail_items: [
//         { pattern: "_addFlag", method: "PUT" },
//         { pattern: "_deleteFlag", method: "POST" },
//         { pattern: "part", method: "GET" }
//     ],
//     mail_folders: [{ pattern: "importItems", method: "PUT" }]
// };

// function isSequentialRequest(url, method) {
//     const key = url.startsWith("mail_items") ? "mail_items" : "mail_folders";
//     return generateImapRequests[key].find(request => url.includes(request.pattern) && method === request.method);
// }

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
    _multipleById: multipleById
};

async function multipleById(request) {
    const ids = JSON.parse(await request.text());
    return await new MailIDB().getMailItems(ids.map(id => ({ internalId: id })));
}

async function filteredChangesetById() {
    const allMailItems = await new MailIDB().getAllMailItems();
    return {
        created: allMailItems
            .sort((item1, item2) => {
                return item2.value.body.date - item1.value.body.date;
            })
            .map(({ internalId: id, version }) => ({ id, version })),
        delete: [],
        updated: [],
        version: 0
    };
}
