import { precacheAndRoute } from "workbox-precaching";
import { registerRoute } from "workbox-routing";

import registerCSSRoute from "./workbox/registerCSSRoute";
import registerImageRoute from "./workbox/registerImageRoute";
import registerScriptRoute from "./workbox/registerScriptRoute";

import { periodicSync } from "./sync";
import { MailIDB } from "./mailIDB";

precacheAndRoute(self.__WB_MANIFEST);
registerCSSRoute();
registerImageRoute();
registerScriptRoute();

periodicSync(["INBOX"]).then(intervals => {
    console.log("Periodic sync register:", intervals);
});

registerRoute(
    /\/api\/mail_items\/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\/(.*)$/,
    async ({ request, params }) => {
        const [uid, method] = params;
        const db = new MailIDB();
        const folder = await db.getSyncedFolder({ uid });
        if (!folder || !getResponseBody[method]) {
            return fetch(request);
        }
        return new Response(JSON.stringify(await getResponseBody[method](request)), {
            headers: {
                fromcache: "true"
            }
        });
    },
    "POST"
);

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
