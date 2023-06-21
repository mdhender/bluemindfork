import { registerRoute } from "workbox-routing";
import { CacheFirst } from "workbox-strategies";

export default function () {
    registerRoute(({ url }) => url.pathname === "/webapp/blank", new CacheFirst());
}
