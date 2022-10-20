import smime from "../smime";

describe("smime", () => {
    describe("isEncrypted", () => {
        test("return true if the message main part is crypted", () => {});
        test("return true if a subpart of the message  is crypted", () => {});
        test("return true if multiple subpart of the message  is crypted", () => {});
        test("return false if there is not crypted subpart", () => {});
    });
    describe("decrypt", () => {
        test("adapt message body structure when the main part is crypted", () => {});
        test("adapt message body structure when a subpart is crypted", () => {});
        test("adapt message body structure when multiple subpart are crypted", () => {});
        test("add decrypted parts content to part cache", () => {});
        test("add a flag / header if the message is crypted", () => {});
        test("add a flag / header if the message is correcty decrypted", () => {});
        test("add a flag / header if the message cannot be decrypted because private key or certificate are not valid", () => {});
        test("add a flag / header if the message cannot be decrypted because private key or certificate are expired", () => {});
        test("add a flag / header if the message cannot be decrypted because private key or certificate are revoked", () => {});
        test("add a flag / header if the message cannot be decrypted because private key or certificate are not trusted", () => {});
    });
});
