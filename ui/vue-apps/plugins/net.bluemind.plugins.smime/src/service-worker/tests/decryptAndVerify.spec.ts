import FDBFactory from "fake-indexeddb/lib/FDBFactory";
import decrypt from "../smime/decrypt";
import decryptAndVerify, { decryptAndVerifyImpl } from "../smime/decryptAndVerify";
import verify from "../smime/verify";
import * as bodyCache from "../smime/cache/BodyCache";
import fetchMock from "fetch-mock";

jest.mock("../smime/cache/BodyCache");
jest.mock("../smime/decrypt", () => jest.fn());
jest.mock("../smime/verify", () => jest.fn());

fetchMock.mock("/session-infos", {
    login: "mathilde.michau@blue-mind.net",
    sid: "58a1ee1b-0c30-492c-a83f-4396f0a24730",
    defaultEmail: "math@devenv.blue"
});

describe("decryptAndVerify", () => {
    let unencrypted: any, encryptedItem: any, signedItem: any;
    beforeEach(() => {
        unencrypted = {
            value: {
                body: { headers: [], structure: { address: "1", mime: "text/html" } },
                imapUid: 1
            }
        };
        encryptedItem = {
            value: {
                body: { headers: [], structure: { address: "1", mime: "application/pkcs7-mime" } },
                imapUid: 99
            }
        };
        signedItem = {
            value: {
                body: {
                    headers: [],
                    structure: {
                        mime: "multipart/signed",
                        children: [{ mime: "multipart/alternative" }, { mime: "application/pkcs7-signature" }]
                    }
                }
            }
        };

        global.indexedDB = new FDBFactory();
        jest.clearAllMocks();
    });

    describe("decrypt and verify caches", () => {
        test("call body cache methods if the item is signed and/or encrypted", async () => {
            await decryptAndVerify([encryptedItem, signedItem], "folderUid");
            expect(bodyCache.setReference).toHaveBeenCalledTimes(2);
            expect(bodyCache.getBody).toHaveBeenCalledTimes(2);
            expect(bodyCache.invalidate).toHaveBeenCalledTimes(1);
        });
        test("does not call body getBody or setReference if the item is not signed and/or encrypted", async () => {
            await decryptAndVerify([unencrypted], "folderUid");
            expect(bodyCache.setReference).not.toHaveBeenCalled();
            expect(bodyCache.getBody).not.toHaveBeenCalled();
        });
    });

    describe("decrypt and verify mailbox item", () => {
        test("decrypt encrypted messages", async () => {
            (<jest.Mock>decrypt).mockResolvedValue({ body: encryptedItem.value.body });
            await decryptAndVerifyImpl(encryptedItem, "folderUid");
            expect(decrypt).toHaveBeenCalledTimes(1);
        });

        test("verify signed messages", async () => {
            await decryptAndVerifyImpl(signedItem, "folderUid");
            expect(verify).toHaveBeenCalledTimes(1);
        });

        test("do nothing if messages are not encrypted or signed", async () => {
            await decryptAndVerifyImpl(unencrypted, "folderUid");
            expect(verify).not.toHaveBeenCalled();
            expect(decrypt).not.toHaveBeenCalled();
        });
    });
});
