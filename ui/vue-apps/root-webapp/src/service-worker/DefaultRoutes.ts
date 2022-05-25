import { ExpirationPlugin } from "workbox-expiration";
import { Route } from "workbox-routing";
import { CacheFirst, NetworkFirst } from "workbox-strategies";

const DefaulRoutes = {
    // CSS : Use cache but update in the background
    STYLES: new Route(({ request }) => request.destination === "style", new NetworkFirst({ cacheName: "css" })),
    // Images: Use cache (with expiration) with a fallback on network
    IMAGES: new Route(
        ({ request }) => request.destination === "image",
        new CacheFirst({
            cacheName: "image",
            plugins: [
                new ExpirationPlugin({
                    maxEntries: 128,
                    maxAgeSeconds: 7 * 24 * 60 * 60
                })
            ]
        })
    ),
    // Scripts: Use network with a fallback on cache
    SCRIPTS: new Route(({ request }) => request.destination === "script", new NetworkFirst())
};

export default DefaulRoutes;
