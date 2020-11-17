import { registerRoute } from "workbox-routing";
import { CacheFirst } from "workbox-strategies";

import { logger } from "../logger";
import { sessionInfos } from "../MailAPI";

const WEBSERVER_HANDLER_BASE_URL = "part/url/";
const strategy = new CacheFirst({ cacheName: "part-cache" });

export default function () {
    registerRoute(matchWebserverPartHandler, fetchPartUsingCoreAPI);
    registerRoute(/\/api\/mail_items\/([^/]+)\/part\/([^/]+)\/([^/?]+)/, strategy);
}

function matchWebserverPartHandler({ url }) {
    return url.pathname.startsWith("/webapp/" + WEBSERVER_HANDLER_BASE_URL);
}

async function fetchPartUsingCoreAPI({ request, url }) {
    try {
        const params = Object.fromEntries(url.searchParams.entries());

        const coreRequest = await buildCoreRequestFromWebserverHandlerUrl(params);
        const response = await strategy.handle({ request: coreRequest });

        const headers = new Headers(response.headers);
        headers.set("Content-Type", params.mime + ";charset=" + params.charset);
        headers.set("Content-Disposition", params.filename ? 'attachment; filename="' + params.filename + '"' : "");

        const cloned = new Response(await response.blob(), { headers });
        return cloned;
    } catch (e) {
        logger.warn("Fail to redirect to Core API.. use webserver part handler instead ", { e });
        return fetch(request);
    }
}

async function buildCoreRequestFromWebserverHandlerUrl(params) {
    const filenameParam = params.filename ? "&?filename=" + params.filename : "";
    const apiCoreUrl = `/api/mail_items/${params.folderUid}/part/${params.imapUid}/${params.address}?encoding=${params.encoding}&?mime=${params.mime}&?charset=${params.charset}${filenameParam}`;

    const { sid } = await sessionInfos.getInstance();
    const fetchParams = {
        headers: {
            "x-bm-apikey": sid
        },
        mode: "cors",
        credentials: "include",
        method: "GET"
    };
    return new Request(apiCoreUrl, fetchParams);
}
