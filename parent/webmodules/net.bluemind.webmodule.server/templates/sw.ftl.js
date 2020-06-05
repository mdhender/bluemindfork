// insert a build version number here ${version} to invalidate the previous installed Service Worker on clients 
const CACHE_NAME = "bm-assets";
const files = ["${files?join("\",\"", "", "\"]")};

self.addEventListener("install", function (event) {
  event.waitUntil(precache(files).then(self.skipWaiting()));
});

self.addEventListener("fetch", function (event) {
  event.respondWith(fromCache(event.request));
});

async function fromCache(request) {
  const matching = await caches.match(request);
  return matching || fetch(request);
}

async function precache(files) {
  const cache = await caches.open(CACHE_NAME);
  return cache.addAll(files);
}
