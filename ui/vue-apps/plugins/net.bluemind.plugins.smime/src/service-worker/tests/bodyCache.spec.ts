import { MessageBody } from "@bluemind/backend.mail.api";
import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME, SIGNED_HEADER_NAME } from "../../lib/constants";
import * as bodyCache from "../smime/cache/BodyCache";
import db from "../smime/cache/SMimeBodyDB";
import * as partCache from "../smime/cache/SMimePartCache";

jest.mock("../smime/cache/SMimeBodyDB");
jest.mock("../smime/cache/SMimePartCache");

const decryptedBody = {
    guid: "1234",
    subject: "hello",
    structure: { mime: "text/html", address: "1" },
    headers: [{ name: ENCRYPTED_HEADER_NAME, values: [CRYPTO_HEADERS.OK.toString()] }]
};

const verifiedBody = {
    structure: { mime: "text/html", address: "1" },
    headers: [{ name: SIGNED_HEADER_NAME, values: [CRYPTO_HEADERS.OK.toString()] }]
};

const errorOnDecryptBody = {
    guid: "999",
    headers: [
        { name: SIGNED_HEADER_NAME, values: [CRYPTO_HEADERS.OK.toString()] },
        { name: ENCRYPTED_HEADER_NAME, values: [CRYPTO_HEADERS.DECRYPT_FAILURE.toString()] }
    ]
};
const errorOnVerifyBody = {
    guid: "888",
    headers: [{ name: SIGNED_HEADER_NAME, values: [CRYPTO_HEADERS.UNTRUSTED_CERTIFICATE.toString()] }]
};

describe("body cache", () => {
    describe("getBody", () => {
        let mappingFunction: () => Promise<MessageBody>;
        beforeEach(() => {
            mappingFunction = jest.fn(() => Promise.resolve({}));
            (<jest.Mock>db.getBody).mockResolvedValue(decryptedBody);
            (<jest.Mock>partCache.checkParts).mockReturnValue(true);
            jest.clearAllMocks();
        });
        test("get existing body in db", async () => {
            const body = await bodyCache.getBody("1234", () => Promise.resolve({}));
            expect(body).toEqual(decryptedBody);
        });
        test("calls mapping function if non existing body in db", async () => {
            (<jest.Mock>db.getBody).mockResolvedValue(undefined);
            await bodyCache.getBody("1234", mappingFunction);
            expect(mappingFunction).toHaveBeenCalled();
        });
        test("calls mapping function if the parts are considered invalid by the checkParts function", async () => {
            (<jest.Mock>partCache.checkParts).mockReturnValue(false);
            await bodyCache.getBody("1234", mappingFunction);
            expect(mappingFunction).toHaveBeenCalled();
        });
        test("does not call mapping function if the parts are considered valid by the checkParts function", async () => {
            await bodyCache.getBody("1234", mappingFunction);
            expect(mappingFunction).not.toHaveBeenCalled();
        });
        test("does not checkParts if no parts have been cache by a decrypt", async () => {
            (<jest.Mock>db.getBody).mockResolvedValue(verifiedBody);
            await bodyCache.getBody("999", mappingFunction);
            expect(partCache.checkParts).not.toHaveBeenCalled();
        });
        test("store body in cache if well verified and well decrypted", async () => {
            (<jest.Mock>db.getBody).mockResolvedValue(undefined);
            mappingFunction = jest.fn(() => Promise.resolve(verifiedBody));
            await bodyCache.getBody("999", mappingFunction);
            expect(db.setBody).toHaveBeenCalled();
        });
        test("do not store body in cache when error on decrypt or on verified", async () => {
            (<jest.Mock>db.getBody).mockResolvedValue(undefined);
            mappingFunction = jest.fn(() => Promise.resolve(errorOnDecryptBody));
            await bodyCache.getBody("999", mappingFunction);

            mappingFunction = jest.fn(() => Promise.resolve(errorOnVerifyBody));
            await bodyCache.getBody("888", mappingFunction);
            expect(db.setBody).not.toHaveBeenCalled();
        });
        test("delete stored body if parts are not considered valid", async () => {
            (<jest.Mock>partCache.checkParts).mockReturnValue(false);
            await bodyCache.getBody("1234", mappingFunction);
            expect(db.deleteBody).toHaveBeenCalled();
        });
        test("do not delete stored body if parts are considered valid", async () => {
            (<jest.Mock>partCache.checkParts).mockReturnValue(true);
            await bodyCache.getBody("1234", mappingFunction);
            expect(db.deleteBody).not.toHaveBeenCalled();
        });
    });
    describe("setReference", () => {
        test("call setGuid with correct parameters", async () => {
            const folderUid = "folderUid";
            const imapUid = 1000;
            const guid = "guid";
            await bodyCache.setReference(folderUid, imapUid, guid);
            expect(db.setGuid).toHaveBeenCalledWith(folderUid, imapUid, guid);
        });
    });
    describe("invalidate", () => {
        test("call invalidate with correct parameters", async () => {
            const today = new Date().getTime();
            const sevenDays = 604800000;
            await bodyCache.invalidate();
            expect(db.invalidate).toHaveBeenCalledWith(today - sevenDays);
        });
    });
});
