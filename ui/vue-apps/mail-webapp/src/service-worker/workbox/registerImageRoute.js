import { registerRoute } from "workbox-routing";
import { CacheFirst } from "workbox-strategies";
import { ExpirationPlugin } from "workbox-expiration";

export default function() {
    registerRoute(
        // Cache image files.
        ({ request }) => request.destination === "image",
        // Use the cache if it's available.
        new CacheFirst({
            // Use a custom cache name.
            cacheName: "image-cache",
            plugins: [
                new ExpirationPlugin({
                    // Cache only 20 images.
                    maxEntries: 20,
                    // Cache for a maximum of a week.
                    maxAgeSeconds: 7 * 24 * 60 * 60
                })
            ]
        })
    );
}
