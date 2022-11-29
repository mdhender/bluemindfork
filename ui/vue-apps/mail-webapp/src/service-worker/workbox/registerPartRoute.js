import { registerRoute } from "workbox-routing";
import { CacheFirst } from "workbox-strategies";

import Session from "../session";

const WEBSERVER_HANDLER_BASE_URL = "/webapp/part/url/";

class PartPlugin {
    cacheKeyWillBeUsed({ request }) {
        const url = new URL(request.url);
        if (url.pathname === WEBSERVER_HANDLER_BASE_URL) {
            const { folderUid, imapUid, address } = Object.fromEntries(url.searchParams.entries());
            return `/api/mail_items/${folderUid}/part/${imapUid}/${address}`;
        } else {
            return url.pathname;
        }
    }
    async requestWillFetch({ request }) {
        const url = new URL(request.url);
        if (url.pathname === WEBSERVER_HANDLER_BASE_URL) {
            const params = Object.fromEntries(url.searchParams.entries());
            return await buildCoreRequestFromWebserverHandlerUrl(params);
        }
        return request;
    }
    async handlerWillRespond({ request, response }) {
        const url = new URL(request.url);
        const { mime, charset, filename } = Object.fromEntries(url.searchParams.entries());
        const headers = new Headers(response.headers);
        headers.set("Content-Type", `${mime};charset=${charset}`);
        if (filename) {
            headers.set("Content-Disposition", `inline; filename="${filename}"`);
        } else {
            headers.set("Content-Disposition", `inline`);
        }
        return new Response(await response.blob(), { headers });
    }
}

const strategy = new CacheFirst({ cacheName: "part-cache", plugins: [new PartPlugin()] });

export default function () {
    registerRoute(matchWebserverPartHandler, strategy);
    registerRoute(/\/api\/mail_items\/([^/]+)\/part\/([^/]+)\/([^/?]+)/, strategy);
}

function matchWebserverPartHandler({ url }) {
    return url.pathname === WEBSERVER_HANDLER_BASE_URL;
}

async function buildCoreRequestFromWebserverHandlerUrl(params) {
    const filenameParam = params.filename ? "&filename=" + params.filename : "";
    const encodedMime = encodeURIComponent(params.mime);
    const apiCoreUrl = `/api/mail_items/${params.folderUid}/part/${params.imapUid}/${params.address}?encoding=${params.encoding}&mime=${encodedMime}&charset=${params.charset}${filenameParam}`;
    const { sid } = await Session.infos();
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
