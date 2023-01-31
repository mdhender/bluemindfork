import fs from "fs";
import path from "path";
import { arrayBufferToBase64 } from "@bluemind/arraybuffer";
import extractSignedData from "../signedDataParser";

describe("extract signed data from an eml", () => {
    test("extract valid informations", done => {
        const newLine = "\r\n";
        const files = readSignedOnlyEmls();
        files.forEach(eml => {
            const { toDigest, pkcs7Part } = extractSignedData(eml);
            const pkcs7Text = arrayBufferToBase64(pkcs7Part);
            expect(pkcs7Text.startsWith(newLine)).toBe(false);
            expect(pkcs7Text.endsWith(newLine)).toBe(false);
            try {
                atob(pkcs7Text);
            } catch {
                done.fail();
            }
            expect(toDigest.startsWith("Content-")).toBe(true); // headers must be digested
            expect(toDigest.endsWith("\r\n\r\n\r\n")).toBe(false); // trailing CRLF is removed
            expect(toDigest.endsWith("\r\n\r\n")).toBe(true);
            done();
        });
    });
});

function readSignedOnlyEmls() {
    return ["corrupted.eml", "valid.eml", "invalid_signature.eml"].map(filename =>
        fs.readFileSync(path.join(__dirname, `./data/eml/signed_only/${filename}`), "utf8")
    );
}
