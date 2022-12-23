import { CacheKeyWillBeUsedCallbackParam, HandlerWillRespondCallbackParam } from "workbox-core";
import { CacheFirst } from "workbox-strategies";

class PartPlugin {
    async cacheKeyWillBeUsed({ request }: CacheKeyWillBeUsedCallbackParam) {
        const url = new URL(request.url);
        return url.pathname;
    }
    async handlerWillRespond({ request, response }: HandlerWillRespondCallbackParam) {
        const url = new URL(request.url);
        const { mime, charset, filename } = Object.fromEntries(url.searchParams.entries());
        const headers = new Headers(response.headers);
        headers.set("Content-Type", `${mime};charset=${charset}`);
        if (filename) {
            headers.set("Content-Disposition", `inline; filename="${encodeURIComponent(filename)}"`);
        } else {
            headers.set("Content-Disposition", `inline`);
        }
        return new Response(await response.blob(), { headers });
    }
}

export default new CacheFirst({ cacheName: "part-cache", plugins: [new PartPlugin()] });
