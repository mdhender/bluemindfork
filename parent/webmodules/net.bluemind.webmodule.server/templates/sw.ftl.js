// insert a build version number here ${version} to invalidate the previous installed Service Worker on clients 
const CACHE_NAME = "bm-assets";
const files = ["${scope}/", "${files?join("\",\"", "", "\"]")};

self.addEventListener("install", function (event) {
  event.waitUntil(precacheredirected(files).then(self.skipWaiting()));
});

self.addEventListener("fetch", function (event) {
  event.respondWith(serve(event.request));
});

async function serve(request) {
	if (request.destination === "document") {
		return fromNetwork(request);
	} else {
		return fromCache(request)
	}
}

async function fromCache(request) {
  const matching = await caches.match(request);
  return matching || fetch(request);
}

async function fromNetwork(request) {
  try {    
    const response = await fetch(request);
    const cache = await caches.open(CACHE_NAME);    
    cache.put(request, response.clone());
    return response;
  } catch {
    return caches.match(request);
  }
}

async function precacheredirected(files) {
    const cache = await caches.open(CACHE_NAME);
    Promise.all(files
      .map(async function(url) {
        const response = await fetch(new Request(url, {redirect: 'manual'})); // Required by https://bugs.chromium.org/p/chromium/issues/detail?id=669363&desc=2#c1
        cache.put(url, response);
      }));
}
