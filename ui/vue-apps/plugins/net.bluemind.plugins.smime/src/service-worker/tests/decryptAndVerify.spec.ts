import FDBFactory from "fake-indexeddb/lib/FDBFactory";
import decryptAndVerify from "../decryptAndVerify";
import * as bodyCache from "../smime/cache/BodyCache";
import fetchMock from "fetch-mock";

jest.mock("../smime/cache/BodyCache");

fetchMock.mock("/session-infos", {
    login: "mathilde.michau@blue-mind.net",
    sid: "58a1ee1b-0c30-492c-a83f-4396f0a24730",
    defaultEmail: "math@devenv.blue"
});

describe("decrypt and verify", () => {
    beforeEach(() => {
        global.indexedDB = new FDBFactory();
        jest.clearAllMocks();
    });
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

const unencrypted = {
    value: {
        body: { headers: [], structure: { address: "1", mime: "text/html" } },
        imapUid: 1
    }
};

const encryptedItem = {
    value: {
        body: { headers: [], structure: { address: "1", mime: "application/pkcs7-mime" } },
        imapUid: 99
    }
};
const signedItem = {
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
