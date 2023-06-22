import { Route } from "workbox-routing";
import { dispatchFetch, fetchRequest } from "@bluemind/service-worker-utils";
import session from "@bluemind/session";

// TODO : try to make a real type from fetch parameters should be possible...
// type FetchParameters =  Parameters<MailboxItemsClient["fetch"]>; => [folderUid: string....]
type FetchParameters = {
    folderUid: string;
    imapUid: number;
    address: string;
    encoding?: string;
    mime?: string;
    charset?: string;
    filename?: string;
};
export const PartRoute = new Route(
    ({ url: { pathname } }) => pathname === "/webapp/part/url/",
    async ({ request }) => {
        const url = new URL(request.url);
        const query = Object.fromEntries(url.searchParams.entries()) as { [k in keyof FetchParameters]: string };
        const params = { ...query, imapUid: parseInt(query.imapUid) };
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
);

async function buildCoreRequestFromWebserverHandlerUrl(params: FetchParameters) {
    const { folderUid, imapUid, address, encoding, mime, charset, filename } = params;
    return fetchRequest(await session.sid, folderUid, imapUid, address, encoding, mime, charset, filename);
}
