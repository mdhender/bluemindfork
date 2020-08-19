import { registerRoute } from "workbox-routing";
import { NetworkFirst } from "workbox-strategies";

export default function () {
    registerRoute(({ request }) => request.destination === "script", new NetworkFirst());
}
