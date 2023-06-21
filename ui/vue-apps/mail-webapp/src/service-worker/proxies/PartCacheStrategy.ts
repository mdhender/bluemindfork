import { CacheKeyWillBeUsedCallbackParam } from "workbox-core";
import { CacheFirst } from "workbox-strategies";

class PartPlugin {
    async cacheKeyWillBeUsed({ request }: CacheKeyWillBeUsedCallbackParam) {
        const url = new URL(request.url);
        return url.pathname;
    }
}

export default new CacheFirst({ cacheName: "part-cache", plugins: [new PartPlugin()] });
