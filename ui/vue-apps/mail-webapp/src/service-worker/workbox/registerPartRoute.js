import { registerRoute } from "workbox-routing";
import { dispatchFetch, fetchRequest } from "@bluemind/service-worker-utils";
import Session from "../session";

const WEBSERVER_HANDLER_BASE_URL = "/webapp/part/url/";

export default function () {
    registerRoute(matchWebserverPartHandler, webserverUrlHandler);
}

function matchWebserverPartHandler({ url }) {
    return url.pathname === WEBSERVER_HANDLER_BASE_URL;
}

async function webserverUrlHandler({ request }) {
    const url = new URL(request.url);
    const params = Object.fromEntries(url.searchParams.entries());
    const coreRequest = await buildCoreRequestFromWebserverHandlerUrl(params);
    const response = await dispatchFetch(coreRequest);
    const { mime, charset, filename } = params;
    const headers = new Headers(response.headers);
    headers.set("Content-Type", `${mime};charset=${charset}`);
    if (filename) {
        headers.set("Content-Disposition", `inline; filename="${encodeURIComponent(filename)}"`);
    } else {
        headers.set("Content-Disposition", `inline`);
    }
    return new Response(await response.blob(), { headers });
}

async function buildCoreRequestFromWebserverHandlerUrl(params) {
    const { sid } = await Session.infos();
    const { folderUid, imapUid, address, encoding, mime, charset, filename } = params;
    return fetchRequest(sid, folderUid, imapUid, address, encoding, mime, charset, filename);
}
