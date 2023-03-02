/// <reference lib="WebWorker" />
declare let self: ServiceWorkerGlobalScope;

/**
 * Dispatch a new fetch event with a given request.
 * Enable the request to pass through all fetch event listeners, including those of the service worker
 */
export function dispatchFetch(request: Request): Promise<Response> {
    return new Promise(resolve => {
        const fetchEvent = new FetchEvent("fetch", { request });
        fetchEvent.waitUntil = () => false;
        const callback = (event: FetchEvent) => {
            if (event === fetchEvent) {
                self.removeEventListener("fetch", callback);
                resolve(fetch(request));
            }
        };
        self.addEventListener("fetch", callback);
        fetchEvent.respondWith = (response: Response) => {
            self.removeEventListener("fetch", callback);
            resolve(response);
        };
        self.dispatchEvent(fetchEvent);
    });
}

export function fetchRequest(
    sid: string,
    folderUid: string,
    imapUid: number,
    address: string,
    encoding: string,
    mime: string,
    charset: string,
    filename?: string
): Request {
    const filenameParam = filename ? "&filename=" + filename : "";
    const encodedMime = encodeURIComponent(mime!);
    const apiCoreUrl = `/api/mail_items/${folderUid}/part/${imapUid}/${address}?encoding=${encoding}&mime=${encodedMime}&charset=${charset}${filenameParam}`;
    const fetchParams: RequestInit = {
        headers: {
            "x-bm-apikey": sid
        },
        mode: "cors",
        credentials: "include",
        method: "GET"
    };
    return new Request(apiCoreUrl, fetchParams);
}
