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
