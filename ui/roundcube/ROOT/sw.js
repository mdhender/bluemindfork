
const UNREAD_CACHE_TIMEOUT = 10 * 1000; 
self.addEventListener("install", function (event) {
    self.skipWaiting();
});

self.addEventListener("fetch", function (event) {
    if (/\/api\/mailboxes\/.*\/_unread$/.test(event.request.url)) {
        event.respondWith(fromUnreadCache(event.request));
    }
});

async function fromUnreadCache(request) {
    const cache = await caches.open("bm-api-legacy");
    let response = await cache.match(request);
    if(!isUnreadResponseValid(response)) {
        response = await fetch(request);
        if(!isUnreadResponseValid(response)) {
            response = new Response(response);
            response.headers.set(date, new Date().toString());
        }
        cache.put(request, response.clone());
    }
    return response
}


function isUnreadResponseValid(response) {
    if (!response || !response.headers.has("date")) {
        return false;
    }
    const date = new Date(response.headers.get("date"));
    if (date instanceof Date && !isNaN(date)) {
        return Date.now() - date.getTime() < UNREAD_CACHE_TIMEOUT
    }

}