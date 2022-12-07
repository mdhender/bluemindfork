import { registerRoute } from "workbox-routing";
import { extensions } from "@bluemind/extensions";
import { clearMyCryptoFiles, getMyStatus, setMyCertificate, setMyPrivateKey } from "./pki";
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
    return new Response(await getMyStatus());
}

async function deleteCryptoFilesHandler() {
    await clearMyCryptoFiles();
    return new Response();
}

async function setPrivateKey({ request }) {
    const blob = await request.blob();
    await setMyPrivateKey(blob);
    return new Response();
}

async function setCertificate({ request }) {
    const blob = await request.blob();
    await setMyCertificate(blob);
    return new Response();
}
