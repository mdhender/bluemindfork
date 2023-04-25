import { registerRoute } from "workbox-routing";
import { NetworkFirst } from "workbox-strategies";

export default function () {
    registerRoute(
        // Cache style resources, i.e. CSS files.
        ({ request }) => request.destination === "style",
        // Use cache but update in the background.
        new NetworkFirst({
            // Use a custom cache name.
            cacheName: "css-cache"
        })
    );
}
