import { MailboxItem } from "@bluemind/backend.mail.api";
import { ItemValue } from "@bluemind/core.container.api";
import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME, SIGNED_HEADER_NAME } from "../../lib/constants";
import decrypt from "../smime/decrypt";
import decryptAndVerify from "../smime/decryptAndVerify";
import encrypt from "../smime/encrypt";
import sign from "../smime/sign";
import SMimeApiProxy from "../SMimeApiProxy";

jest.mock("../smime/decrypt", () => jest.fn(() => ({ body: { structure: {} } })));
jest.mock("../smime/decryptAndVerify", () => jest.fn(() => []));
jest.mock("../smime/encrypt", () => jest.fn());
jest.mock("../smime/sign", () => jest.fn(item => item));

jest.mock("../environnment/session", () =>
    Promise.resolve({
        json: () =>
            Promise.resolve({
                login: "mathilde.michau@blue-mind.net",
                sid: "58a1ee1b-0c30-492c-a83f-4396f0a24730"
            })
    })
);

describe("SMimeApiProxy", () => {
    const smimeApiProxy = new SMimeApiProxy("apiKey", "folderUid");

    describe("catch multipleGetById & getCompleteById requests", () => {
        beforeEach(() => {
            smimeApiProxy.next = () => Promise.resolve(<never>[encryptedItem]);
            signedItem = getSignedItem();
        });

        test("call decrypt and verify", async () => {
            smimeApiProxy.next = () => Promise.resolve(<never>[encryptedItem]);
            await smimeApiProxy.multipleGetById();
            smimeApiProxy.next = () => Promise.resolve(<never>[encryptedItem]);
            await smimeApiProxy.getCompleteById();
            expect(decryptAndVerify).toHaveBeenCalledTimes(2);
        });

        test("call next to get messages metadata", async () => {
            smimeApiProxy.next = jest.fn(() => Promise.resolve(<never>item));
            await smimeApiProxy.multipleGetById();
            await smimeApiProxy.getCompleteById();
            expect(smimeApiProxy.next).toHaveBeenCalledTimes(2);
        });
    });

    describe("catch getForUpdate request", () => {
        beforeEach(() => {
            jest.clearAllMocks();
        });

        test("call next to get messages metadata", async () => {
            smimeApiProxy.next = jest.fn(() => Promise.resolve(<never>item));
            await smimeApiProxy.getForUpdate();
            expect(smimeApiProxy.next).toHaveBeenCalledTimes(1);
        });
        test("decrypt encrypted message", async () => {
            smimeApiProxy.next = () => Promise.resolve(<never>encryptedItem);
            await smimeApiProxy.getForUpdate();
            expect(decrypt).toHaveBeenCalledTimes(1);
        });
        test("do nothing if message is not encrypted", async () => {
            smimeApiProxy.next = () => Promise.resolve(<never>item);
            await smimeApiProxy.getForUpdate();
            expect(decrypt).not.toHaveBeenCalled();
        });
        test("remove 'application/pkcs7-signature' part (must be hidden in MailComposer)", async () => {
            smimeApiProxy.next = () => Promise.resolve(<never>signedItem);
            const unsignedMimeType = signedItem.value.body.structure!.children![0].mime;
            const readyForUpdate = await smimeApiProxy.getForUpdate();
            expect(readyForUpdate.value.body.structure!.mime).toBe(unsignedMimeType);
        });
    });

    describe("catch create & updateById requests", () => {
        beforeEach(() => {
            signedItem = getSignedItem();
            jest.clearAllMocks();
        });
        test("sign message if requested", async () => {
            // (<jest.Mock>sign).mockResolvedValue(signedItem.value);
            await smimeApiProxy.create(itemToBeSigned);
            await smimeApiProxy.updateById(123, itemToBeSigned);
            expect(sign).toHaveBeenCalledTimes(2);
        });
        test("encrypt message if requested", async () => {
            (<jest.Mock>encrypt).mockResolvedValue(encryptedItem.value);
            await smimeApiProxy.create(itemToBeEncrypted);
            await smimeApiProxy.updateById(123, itemToBeEncrypted);
            expect(encrypt).toHaveBeenCalledTimes(2);
        });
        test("sign & encrypt message if both are requested", async () => {
            (<jest.Mock>sign).mockResolvedValue(itemToBeEncrypted);
            (<jest.Mock>encrypt).mockResolvedValue(encryptedItem.value);
            await smimeApiProxy.create(itemToBeSignedAndEncrypted);
            expect(sign).toHaveBeenCalledTimes(1);
            expect(sign).toHaveBeenCalledWith(itemToBeSignedAndEncrypted, "folderUid");
            expect(encrypt).toHaveBeenCalledTimes(1);
            expect(encrypt).toHaveBeenCalledWith(itemToBeEncrypted, "folderUid");
            jest.clearAllMocks();

            (<jest.Mock>sign).mockResolvedValue(itemToBeEncrypted);
            (<jest.Mock>encrypt).mockResolvedValue(encryptedItem.value);
            await smimeApiProxy.updateById(123, itemToBeSignedAndEncrypted);
            expect(sign).toHaveBeenCalledTimes(1);
            expect(sign).toHaveBeenCalledWith(itemToBeSignedAndEncrypted, "folderUid");
            expect(encrypt).toHaveBeenCalledTimes(1);
            expect(encrypt).toHaveBeenCalledWith(itemToBeEncrypted, "folderUid");
        });
        test("do nothing if neither signature nor encryption is requested", async () => {
            await smimeApiProxy.create(item.value);
            await smimeApiProxy.updateById(123, item.value);
            expect(encrypt).not.toHaveBeenCalled();
            expect(sign).not.toHaveBeenCalled();
        });
        test("call next after message has been altered", async () => {
            smimeApiProxy.next = jest.fn(() => Promise.resolve(<never>{}));
            (<jest.Mock>sign).mockResolvedValue(signedItem.value);
            await smimeApiProxy.create(itemToBeSigned);
            expect(sign).toHaveBeenCalledTimes(1);
            expect(smimeApiProxy.next).toHaveBeenCalledWith(signedItem.value);

            smimeApiProxy.next = jest.fn(() => Promise.resolve(<never>{}));
            (<jest.Mock>encrypt).mockResolvedValue(encryptedItem.value);
            await smimeApiProxy.updateById(123, itemToBeEncrypted);
            expect(encrypt).toHaveBeenCalledTimes(1);
            expect(smimeApiProxy.next).toHaveBeenCalledWith(123, encryptedItem.value);
        });
    });
});

const item = {
    value: {
        body: {
            headers: [],
            structure: { mime: "multipart/alternative" }
        }
    }
};

const encryptedBody = {
    headers: [
        { name: ENCRYPTED_HEADER_NAME, values: [CRYPTO_HEADERS.TO_DO.toString()] },
        { name: SIGNED_HEADER_NAME, values: [CRYPTO_HEADERS.TO_DO.toString()] }
    ],
    structure: { mime: "application/pkcs7-mime" }
};

const encryptedItem = {
    value: {
        body: encryptedBody
    }
};

const itemToBeSigned = {
    body: {
        headers: [{ name: SIGNED_HEADER_NAME, values: [CRYPTO_HEADERS.TO_DO.toString()] }],
        structure: { mime: "multipart/alternative" }
    }
};

const itemToBeEncrypted = {
    body: {
        headers: [{ name: ENCRYPTED_HEADER_NAME, values: [CRYPTO_HEADERS.TO_DO.toString()] }],
        structure: { mime: "multipart/alternative" }
    }
};

const itemToBeSignedAndEncrypted = {
    body: {
        headers: [
            { name: SIGNED_HEADER_NAME, values: [CRYPTO_HEADERS.TO_DO.toString()] },
            { name: ENCRYPTED_HEADER_NAME, values: [CRYPTO_HEADERS.TO_DO.toString()] }
        ],
        structure: { mime: "multipart/alternative" }
    }
};
let signedItem: ItemValue<MailboxItem>;
const getSignedItem = () => ({
    value: {
        body: {
            headers: [],
            structure: {
                mime: "multipart/signed",
                children: [{ mime: "multipart/alternative" }, { mime: "application/pkcs7-signature" }]
            }
        }
    }
});
