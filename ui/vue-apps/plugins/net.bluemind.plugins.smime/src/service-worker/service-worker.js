import { registerRoute } from "workbox-routing";
import { extensions } from "@bluemind/extensions";
import SmimeDB from "./SmimeDB";
import SmimeHandler from "./SmimeHandler";

extensions.register("serviceworker.handlers", "smime-plugin", {
    "api-handler": { class: SmimeHandler, priority: 256 }
});

registerRoute(matchManageSmimeKeyRoute, hasCryptoFilesHandler, "GET");
registerRoute(matchManageSmimeKeyRoute, deleteCryptoFilesHandler, "DELETE");
registerRoute(matchManageSmimeKeyRoute, setCryptoFilesHandler, "PUT");

// FIXME: with service-worker global env
const SW_INTERNAL_API_PATH = "/service-worker-internal/";

function matchManageSmimeKeyRoute({ url }) {
    return url.pathname === SW_INTERNAL_API_PATH + "smime";
}

async function hasCryptoFilesHandler() {
    const has = await SmimeDB.hasCryptoFiles();
    console.log(has);
    console.log();
    return new Response(JSON.stringify(has));
}

async function deleteCryptoFilesHandler() {
    await SmimeDB.deleteCryptoFiles();
    return new Response();
}

async function setCryptoFilesHandler({ request, url }) {
    const blob = await request.blob();
    const kind = url.searchParams.get("kind");
    const storeFn = kind === "privateKey" ? SmimeDB.setPrivateKey : SmimeDB.setPublicCert;
    await storeFn(blob);
    return new Response();
}
