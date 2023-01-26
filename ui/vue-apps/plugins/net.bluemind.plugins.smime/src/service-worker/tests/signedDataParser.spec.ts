import fs from "fs";
import path from "path";
import extractSignedData from "../signedDataParser";

describe("extract signed data from an eml", () => {
    function arrayBufferToBase64(buffer: ArrayBuffer) {
        let binary = "";
        const bytes = new Uint8Array(buffer);
        const len = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return window.btoa(binary);
    }

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
