import { isTextHtmlSignatureEmpty } from "../signature";

describe("Signature model", () => {
    describe("Check empty signature", () => {
        test("empty string", () => {
            expect(isTextHtmlSignatureEmpty("")).toBe(true);
        });
        test("null", () => {
            expect(isTextHtmlSignatureEmpty(null)).toBe(true);
        });
        test("simple string", () => {
            expect(isTextHtmlSignatureEmpty("blabla")).toBe(false);
        });
        test("New signature, no text", () => {
            const emptySignatureWithNewEditor =
                '<div id="bm-composer-content-wrapper"><p style="margin-bottom: 0; margin-top: 0;"><br></p><style></style></div>';
            expect(isTextHtmlSignatureEmpty(emptySignatureWithNewEditor)).toBe(true);
        });
        test("Old signature, no text", () => {
            const emptySignatureWithOldEditor = "<pre></pre>";
            expect(isTextHtmlSignatureEmpty(emptySignatureWithOldEditor)).toBe(true);
        });
        test("New signature, with text", () => {
            const signatureWithNewEditor =
                '<div id="bm-composer-content-wrapper"><p style="margin-bottom: 0; margin-top: 0;"><br>blabla</p><style></style></div>';
            expect(isTextHtmlSignatureEmpty(signatureWithNewEditor)).toBe(false);
        });
        test("Old signature, with text", () => {
            const signatureWithOldEditor = "<pre>blabla</pre>";
            expect(isTextHtmlSignatureEmpty(signatureWithOldEditor)).toBe(false);
        });
        test("New signature, with valid Img tag", () => {
            const signatureWithImgNewEditor =
                '<div id="bm-composer-content-wrapper"><p style="margin-bottom: 0; margin-top: 0;"><br><img src="http://toto/toto.gif"></p><style></style></div>';
            expect(isTextHtmlSignatureEmpty(signatureWithImgNewEditor)).toBe(false);
        });
        test("New signature, with not-valid Img tag", () => {
            const signatureWithImgNewEditor =
                '<div id="bm-composer-content-wrapper"><p style="margin-bottom: 0; margin-top: 0;"><br><img plop="http://toto/toto.gif"></p><style></style></div>';
            expect(isTextHtmlSignatureEmpty(signatureWithImgNewEditor)).toBe(true);
        });
        test("New signature, with image background", () => {
            const signatureWithImgNewEditor =
                '<div id="bm-composer-content-wrapper"><p style="margin-bottom: 0; margin-top: 0;"><br><span style=" height: 30vh; background-image: http://toto/toto.gif;">plop</span></p><style></style></div>';
            expect(isTextHtmlSignatureEmpty(signatureWithImgNewEditor)).toBe(false);
        });
    });
});
