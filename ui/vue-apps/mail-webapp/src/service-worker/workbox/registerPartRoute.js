import { registerRoute } from "workbox-routing";
import { dispatchFetch } from "@bluemind/service-worker-utils";
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
    return dispatchFetch(coreRequest);
}

async function buildCoreRequestFromWebserverHandlerUrl(params) {
    const { sid } = await Session.infos();
    const filenameParam = params.filename ? "&filename=" + params.filename : "";
    const encodedMime = encodeURIComponent(params.mime);
    const apiCoreUrl = `/api/mail_items/${params.folderUid}/part/${params.imapUid}/${params.address}?encoding=${params.encoding}&mime=${encodedMime}&charset=${params.charset}${filenameParam}`;
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
