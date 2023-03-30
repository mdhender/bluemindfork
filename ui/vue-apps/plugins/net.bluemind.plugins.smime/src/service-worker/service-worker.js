import { pki } from "node-forge";
import { registerRoute } from "workbox-routing";
import { extensions } from "@bluemind/extensions";
import BmRoles from "@bluemind/roles";
import { checkCertificate, clear as clearPki, getMyStatus, setMyCertificate, setMyPrivateKey } from "./pki";
import bodyDB from "./smime/cache/SMimeBodyDB";
import SMimeApiProxy from "./SMimeApiProxy";
import { PKIEntry, SMIME_INTERNAL_API_URL, SMIME_UNTRUSTED_CERTIFICATE_ERROR_PREFIX } from "../lib/constants";

extensions.register("serviceworker.handlers", "smime-plugin", {
    "api-handler": { class: SMimeApiProxy, priority: 256, role: BmRoles.CAN_USE_SMIME }
});

registerRoute(SMIME_INTERNAL_API_URL, hasCryptoFilesHandler, "GET");
registerRoute(SMIME_INTERNAL_API_URL, clearPkiAndCache, "DELETE");
registerRoute(`${SMIME_INTERNAL_API_URL}/${PKIEntry.PRIVATE_KEY}`, setPrivateKey, "PUT");
const setCertificateMatcher = ({ url }) => {
    return url.pathname === `${SMIME_INTERNAL_API_URL}/${PKIEntry.CERTIFICATE}`;
};
registerRoute(setCertificateMatcher, setCertificate, "PUT");

async function hasCryptoFilesHandler() {
    return new Response(await getMyStatus());
}

async function clearPkiAndCache() {
    await clearPki();
    await bodyDB.clearBody();
    await bodyDB.clearGuid();
    await caches.delete("smime-part-cache");
    return new Response();
}

async function setPrivateKey({ request }) {
    const blob = await request.blob();
    await setMyPrivateKey(blob);
    return new Response();
}

async function setCertificate({ request }) {
    const expectedEmail = new URL(request.url).searchParams.get("email");
    const blob = await request.blob();
    const cert = await blob.text();
    try {
        await checkCertificate(pki.certificateFromPem(cert), new Date(), expectedEmail);
    } catch (error) {
        const errorMessage = `[${SMIME_UNTRUSTED_CERTIFICATE_ERROR_PREFIX}:${error.code}]` + error.message;
        return new Response(errorMessage, { status: 500 });
    }
    await setMyCertificate(blob);
    return new Response();
}
