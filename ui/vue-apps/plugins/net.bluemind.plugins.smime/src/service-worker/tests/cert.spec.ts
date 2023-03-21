import fetchMock from "fetch-mock";
import { pki } from "node-forge";
import { RevocationResult } from "@bluemind/smime.cacerts.api";
import { readFile } from "./helpers";
import { checkRevoked, getRevocationCacheValidity } from "../pki/cert";

fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });

const aliceCert = readFile("certificates/alice.crt"); // alice cert from RFC9216
const aliceCertificate = pki.certificateFromPem(aliceCert);

describe("cert", () => {
    test("revocation results are cached", async () => {
        const route = "end:/api/smime_revocation/foo.bar/is_revoked";
        fetchMock.mock(
            route,
            [{ status: RevocationResult.RevocationStatus.NOT_REVOKED, serialNumber: aliceCertificate.serialNumber }],
            { overwriteRoutes: true }
        );
        await checkRevoked(aliceCertificate);
        expect(fetchMock.calls(route).length).toEqual(1);

        await checkRevoked(aliceCertificate);
        expect(fetchMock.calls(route).length).toEqual(1);
    });

    test("if certificate is not revoked, cache is valid 7 days", async () => {
        const now = new Date();
        const validity = getRevocationCacheValidity({
            status: RevocationResult.RevocationStatus.NOT_REVOKED,
            serialNumber: ""
        });
        const diff = validity.getTime() - now.getTime();

        const days = Math.ceil(diff / (1000 * 3600 * 24));
        expect(days).toBe(7);
    });

    test("if certificate is revoked but for 'certificateHold' reason, cache is valid only one day", async () => {
        const now = new Date();
        const validity = getRevocationCacheValidity({
            status: RevocationResult.RevocationStatus.REVOKED,
            reason: "certificateHold",
            serialNumber: ""
        });
        const diff = validity.getTime() - now.getTime();

        const days = Math.ceil(diff / (1000 * 3600 * 24));
        expect(days).toBe(1);
    });

    test("if certificate is revoked, cache is valid forever", async () => {
        const MAX_TIMESTAMP = 8640000000000000;

        const validity = getRevocationCacheValidity({
            status: RevocationResult.RevocationStatus.REVOKED,
            serialNumber: ""
        });

        expect(validity.getTime()).toBe(MAX_TIMESTAMP);
    });
});
