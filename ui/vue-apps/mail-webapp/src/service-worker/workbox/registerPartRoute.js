import { registerRoute } from "workbox-routing";
import { CacheFirst } from "workbox-strategies";

export default function () {
    registerRoute(
        /\/api\/mail_items\/([^/]+)\/part\/([^/]+)\/([^/?]+)/,
        new CacheFirst({
            cacheName: "part-cache"
        })
    );
}
