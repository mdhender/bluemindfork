import fs from "fs";
import path from "path";
import { arrayBufferToBase64 } from "@bluemind/arraybuffer";
import extractSignedData, { extractBoundary } from "../signedDataParser";

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

describe("extract multipart/signed boundary value from an eml", () => {
    test("standard boundary", () => {
        const eml = `MIME-Version: 1.0
        Content-Type: multipart/signed; protocol="application/pkcs7-signature"; 
            micalg=sha-256; boundary="ms020109040004000402080205"
        X-Anything: 1667475017557
        
        --ms020109040004000402080205
        Content-Type: multipart/alternative; boundary="another-boundary"

        ...

        --ms020109040004000402080205--
        `;
        const boundaryValue = extractBoundary(eml);
        expect(boundaryValue).toBe("--ms020109040004000402080205");
    });

    test("boundary without any quote", () => {
        const eml = `MIME-Version: 1.0
        Content-Type: multipart/signed; protocol="application/pkcs7-signature"; micalg=sha-256; boundary=no-quote-for-this-boundary
        X-Anything: 1667475017557
        
        --no-quote-for-this-boundary
        Content-Type: multipart/alternative; boundary="another-boundary"

        ...
        
        --no-quote-for-this-boundary--
        `;
        const boundaryValue = extractBoundary(eml);
        expect(boundaryValue).toBe("--no-quote-for-this-boundary");
    });
});

function readSignedOnlyEmls() {
    return ["corrupted.eml", "valid.eml", "invalid_signature.eml"].map(filename =>
        fs.readFileSync(path.join(__dirname, `./data/eml/signed_only/${filename}`), "utf8")
    );
}
