import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME, SIGNED_HEADER_NAME } from "../../lib/constants";
import smime from "../smime";
import SMimeApiProxy from "../SMimeApiProxy";

jest.mock("../smime", () => jest.fn);

describe("SMimeApiProxy", () => {
    const smimeApiProxy = new SMimeApiProxy("apiKey", "folderUid");
    beforeEach(() => {
        smime.isEncrypted = () => false;
        smime.isSigned = () => false;
        jest.clearAllMocks();
    });

    describe("catch multipleGetById & getCompleteById requests", () => {
        beforeEach(() => {
            smimeApiProxy.next = () => Promise.resolve(<never>[encryptedItem]);
        });

        test("decrypt encrypted messages", async () => {
            smime.isEncrypted = () => true;
            smime.decrypt = jest.fn(() => Promise.resolve({ item: decryptedItem, content: "" }));

            smimeApiProxy.next = () => Promise.resolve(<never>[encryptedItem]);
            await smimeApiProxy.multipleGetById();
            smimeApiProxy.next = () => Promise.resolve(<never>encryptedItem);
            await smimeApiProxy.getCompleteById();

            expect(smime.decrypt).toHaveBeenCalledTimes(2);
        });

        test("verify signed messages", async () => {
            smime.isSigned = () => true;
            smime.verify = jest.fn(() => Promise.resolve(item));

            smimeApiProxy.next = () => Promise.resolve(<never>[signedItem]);
            await smimeApiProxy.multipleGetById();
            smimeApiProxy.next = () => Promise.resolve(<never>signedItem);
            await smimeApiProxy.getCompleteById();

            expect(smime.verify).toHaveBeenCalledTimes(2);
        });
        test("do nothing if messages are not encrypted or signed", async () => {
            smime.isSigned = () => false;
            smime.isEncrypted = () => false;

            smimeApiProxy.next = () => Promise.resolve(<never>[item]);
            await smimeApiProxy.multipleGetById();
            smimeApiProxy.next = () => Promise.resolve(<never>item);
            await smimeApiProxy.getCompleteById();

            expect(smime.verify).not.toHaveBeenCalled();
            expect(smime.decrypt).not.toHaveBeenCalled();
        });
        test("call next to get messages metadata", async () => {
            smimeApiProxy.next = jest.fn(() => Promise.resolve(<never>item));
            await smimeApiProxy.multipleGetById();
            await smimeApiProxy.getCompleteById();
            expect(smimeApiProxy.next).toHaveBeenCalledTimes(2);
        });
    });

    describe("catch getForUpdate request", () => {
        test("call next to get messages metadata", async () => {
            smimeApiProxy.next = jest.fn(() => Promise.resolve(<never>item));
            await smimeApiProxy.getForUpdate();
            expect(smimeApiProxy.next).toHaveBeenCalledTimes(1);
        });
        test("decrypt encrypted message", async () => {
            smime.isEncrypted = () => true;
            smime.decrypt = jest.fn(() => Promise.resolve({ item: decryptedItem, content: "" }));

            smimeApiProxy.next = () => Promise.resolve(<never>encryptedItem);
            await smimeApiProxy.getForUpdate();

            expect(smime.decrypt).toHaveBeenCalledTimes(1);
        });
        test("do nothing if message is not encrypted", async () => {
            smime.isEncrypted = () => false;

            smimeApiProxy.next = () => Promise.resolve(<never>item);
            await smimeApiProxy.getForUpdate();

            expect(smime.decrypt).not.toHaveBeenCalled();
        });
        test("remove 'application/pkcs7-signature' part (must be hidden in MailComposer)", async () => {
            smimeApiProxy.next = () => Promise.resolve(<never>signedItem);
            const unsignedMimeType = signedItem.value.body.structure.children[0].mime;
            const readyForUpdate = await smimeApiProxy.getForUpdate();
            expect(readyForUpdate.value.body.structure!.mime).toBe(unsignedMimeType);
        });
    });
    describe("catch create & updateById requests", () => {
        test("sign message if requested", async () => {
            smime.sign = jest.fn(() => Promise.resolve(signedItem.value));
            await smimeApiProxy.create(itemToBeSigned);
            await smimeApiProxy.updateById(123, itemToBeSigned);
            expect(smime.sign).toHaveBeenCalledTimes(2);
        });
        test("encrypt message if requested", async () => {
            smime.encrypt = jest.fn(() => Promise.resolve(encryptedItem.value));
            await smimeApiProxy.create(itemToBeEncrypted);
            await smimeApiProxy.updateById(123, itemToBeEncrypted);
            expect(smime.encrypt).toHaveBeenCalledTimes(2);
        });
        test("sign & encrypt message if both are requested", async () => {
            smime.sign = jest.fn(() => Promise.resolve(itemToBeEncrypted));
            smime.encrypt = jest.fn(() => Promise.resolve(encryptedItem.value));
            await smimeApiProxy.create(itemToBeSignedAndEncrypted);
            expect(smime.sign).toHaveBeenCalledTimes(1);
            expect(smime.sign).toHaveBeenCalledWith(itemToBeSignedAndEncrypted, "folderUid");
            expect(smime.encrypt).toHaveBeenCalledTimes(1);
            expect(smime.encrypt).toHaveBeenCalledWith(itemToBeEncrypted, "folderUid");

            smime.sign = jest.fn(() => Promise.resolve(itemToBeEncrypted));
            smime.encrypt = jest.fn(() => Promise.resolve(encryptedItem.value));
            await smimeApiProxy.updateById(123, itemToBeSignedAndEncrypted);
            expect(smime.sign).toHaveBeenCalledTimes(1);
            expect(smime.sign).toHaveBeenCalledWith(itemToBeSignedAndEncrypted, "folderUid");
            expect(smime.encrypt).toHaveBeenCalledTimes(1);
            expect(smime.encrypt).toHaveBeenCalledWith(itemToBeEncrypted, "folderUid");
        });
        test("do nothing if neither signature nor encryption is requested", async () => {
            await smimeApiProxy.create(item.value);
            await smimeApiProxy.updateById(123, item.value);
            expect(smime.encrypt).not.toHaveBeenCalled();
            expect(smime.sign).not.toHaveBeenCalled();
        });
        test("call next after message has been altered", async () => {
            smimeApiProxy.next = jest.fn(() => Promise.resolve(<never>{}));
            smime.sign = jest.fn(() => Promise.resolve(signedItem.value));
            await smimeApiProxy.create(itemToBeSigned);
            expect(smime.sign).toHaveBeenCalledTimes(1);
            expect(smimeApiProxy.next).toHaveBeenCalledWith(signedItem.value);

            smimeApiProxy.next = jest.fn(() => Promise.resolve(<never>{}));
            smime.encrypt = jest.fn(() => Promise.resolve(encryptedItem.value));
            await smimeApiProxy.updateById(123, itemToBeEncrypted);
            expect(smime.encrypt).toHaveBeenCalledTimes(1);
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

const encryptedItem = {
    value: {
        body: {
            headers: [],
            structure: { mime: "application/pkcs7-mime" }
        }
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

const decryptedItem = {
    value: {
        body: {
            headers: [],
            structure: { mime: "multipart/alternative" }
        }
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
