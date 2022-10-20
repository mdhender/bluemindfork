import { registerRoute } from "workbox-routing";
import { extensions } from "@bluemind/extensions";
import db from "./SMimeDB";
import SMimeApiProxy from "./SMimeApiProxy";
import { PKIEntry, SMIME_INTERNAL_API_URL } from "../lib/constants";

extensions.register("serviceworker.handlers", "smime-plugin", {
    "api-handler": { class: SMimeApiProxy, priority: 256 }
});

registerRoute(SMIME_INTERNAL_API_URL, hasCryptoFilesHandler, "GET");
registerRoute(SMIME_INTERNAL_API_URL, deleteCryptoFilesHandler, "DELETE");
registerRoute(`${SMIME_INTERNAL_API_URL}/${PKIEntry.PRIVATE_KEY}`, setPrivateKey, "PUT");
registerRoute(`${SMIME_INTERNAL_API_URL}/${PKIEntry.CERTIFICATE}`, setCertificate, "PUT");

async function hasCryptoFilesHandler() {
    return new Response(await db.getPKIStatus());
}

async function deleteCryptoFilesHandler() {
    await db.clearPKI();
    return new Response();
}

async function setPrivateKey({ request }) {
    db.setPrivateKey(await request.blob());
    return new Response();
}

async function setCertificate({ request }) {
    db.setCertificate(await request.blob());
    return new Response();
}
