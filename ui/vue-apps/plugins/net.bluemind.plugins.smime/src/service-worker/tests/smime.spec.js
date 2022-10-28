import smime from "../smime";
import fs from "fs";
import path from "path";
import pkcs7 from "../pkcs7";
import {
    ExpiredCredentialsError,
    InvalidCredentialsError,
    RevokedCrendentialsError,
    UnmatchedRecipientError,
    UntrustedCredentialsError
} from "../exceptions";
import { CRYPTO_HEADER_NAME } from "../constants";

jest.mock("../pki", () => ({
    getMyPrivateKey: () => Promise.resolve("PrivateKey"),
    getMyCertificate: () => Promise.resolve("MyCertificate")
}));
jest.mock("@bluemind/backend.mail.api", () => ({
    MailboxItemsClient: () => ({
        fetch: () => Promise.resolve("data")
    })
}));
jest.mock("../pkcs7", () => jest.fn);

let mockCache = {};
global.caches = {
    open: () => {
        return Promise.resolve({
            put: (url, response) => {
                mockCache[url] = response;
            }
        });
    }
};
global.fetch = jest.fn(() =>
    Promise.resolve({
        json: () =>
            Promise.resolve({
                login: "mathilde.michau@blue-mind.net",
                sid: "58a1ee1b-0c30-492c-a83f-4396f0a24730"
            }),
        ok: () => true
    })
);
class MockRequest {
    constructor(url) {
        this.url = url;
    }
}
class MockResponse {}
global.Request = MockRequest;
global.Response = MockResponse;

const mainEncrypted = {
    value: {
        body: {
            structure: {
                address: "1",
                mime: "application/pkcs7-mime"
            },
            headers: []
        },
        imapUid: 99
    }
};

const unecrypted = {
    value: {
        body: {
            structure: {
                mime: "multipart/alternative",
                address: "TEXT",
                children: [{ mime: "text/plain" }, { mime: "text/html" }]
            },
            headers: []
        }
    }
};
describe("smime", () => {
    beforeEach(() => {
        pkcs7.decrypt = jest.fn(() => Promise.resolve("content"));
    });

    describe("isEncrypted", () => {
        test("return true if the message main part is crypted", () => {
            const isEncrypted = smime.isEncrypted(mainEncrypted);
            expect(isEncrypted).toBe(true);
        });
        test("return true if a subpart of the message  is crypted", () => {});
        test("return true if multiple subpart of the message  is crypted", () => {});
        test("return false if there is not crypted subpart", () => {
            const isEncrypted = smime.isEncrypted(unecrypted);
            expect(isEncrypted).toBe(false);
        });
    });
    describe("decrypt", () => {
        beforeEach(() => {
            mainEncrypted.value.body.headers = [];
            mockCache = {};
        });
        test("adapt message body structure when the main part is crypted", async () => {
            const structure = await smime.decrypt("uid", mainEncrypted);
            expect(structure.value.body.structure).toEqual(
                expect.objectContaining({ mime: "text/plain", address: "1" })
            );
        });
        test("adapt message body structure when a subpart is crypted", () => {});
        test("adapt message body structure when multiple subpart are crypted", () => {});

        test("add decrypted parts content to part cache", async () => {
            const mockEmlMultipleParts = readEml("unencrypted");
            pkcs7.decrypt = jest.fn(() => Promise.resolve(mockEmlMultipleParts));

            await smime.decrypt("uid", mainEncrypted);
            const result = [
                "/api/mail_items/uid/part/99/1.1.1?encoding=quoted-printable&mime=text%2Fplain&charset=utf-8",
                "/api/mail_items/uid/part/99/1.1.2.1?encoding=quoted-printable&mime=text%2Fhtml&charset=utf-8",
                "/api/mail_items/uid/part/99/1.1.2.2?encoding=base64&mime=image%2Fpng&charset=us-ascii&filename=AUDIO.png",
                "/api/mail_items/uid/part/99/1.2?encoding=base64&mime=image%2Fgif&charset=us-ascii&filename=chooser.gif"
            ];
            expect(Object.keys(mockCache)).toEqual(result);
        });
        test("add a header if the message is crypted", async () => {
            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[0]).toEqual(smime.CRYPTO_HEADERS.ENCRYPTED);
        });
        test("add a header if the message is correcty decrypted", async () => {
            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[1]).toEqual(smime.CRYPTO_HEADERS.DECRYPTED);
        });
        test("add a header if the message cannot be decrypted because private key or certificate are not valid", async () => {
            pkcs7.decrypt = jest.fn(() => Promise.reject(new InvalidCredentialsError()));

            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[1]).toEqual(smime.CRYPTO_HEADERS.INVALID_CREDENTIALS);
        });
        test("add a header if the message cannot be decrypted because private key or certificate are expired", async () => {
            pkcs7.decrypt = jest.fn(() => Promise.reject(new ExpiredCredentialsError()));

            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[1]).toEqual(smime.CRYPTO_HEADERS.EXPIRED_CREDENTIALS);
        });
        test("add a header if the message cannot be decrypted because private key or certificate are revoked", async () => {
            pkcs7.decrypt = jest.fn(() => Promise.reject(new RevokedCrendentialsError()));

            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[1]).toEqual(smime.CRYPTO_HEADERS.REVOKED_CREDENTIALS);
        });
        test("add a header if the message cannot be decrypted because private key or certificate are not trusted", async () => {
            pkcs7.decrypt = jest.fn(() => Promise.reject(new UntrustedCredentialsError()));

            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[1]).toEqual(smime.CRYPTO_HEADERS.UNTRUSTED_CREDENTIALS);
        });
        test("add a header if the given certificate does not match any recipient", async () => {
            pkcs7.decrypt = jest.fn(() => Promise.reject(new UnmatchedRecipientError()));

            const item = await smime.decrypt("uid", mainEncrypted);
            expect(getCryptoHeader(item).values[1]).toEqual(smime.CRYPTO_HEADERS.UNMATCHED_RECIPIENTS);
        });
    });
});

function readEml(file) {
    return fs.readFileSync(path.join(__dirname, `./data/${file}.eml`), "utf8", (err, data) => data);
}

function getCryptoHeader(item) {
    return item.value.body.headers.find(h => h.name === CRYPTO_HEADER_NAME);
}
