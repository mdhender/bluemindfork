import { remove } from "../signature/corporateSignature";
import { ISignature } from "../signature/signature.interface";

describe("Corporate Signature", () => {
    describe("remove Signature", () => {
        it("should remove signature from html Content", () => {
            const htmlCOntent =
                '<div>Content of my message <div class="bm-corporate-signature"> ma Signature </div></div>';

            expect(
                remove(htmlCOntent, {
                    corporateSignature: buildCorporate(),
                    disclaimer: null
                })
            ).toEqual("<div>Content of my message </div>");
        });
        it("should replace signature with a placeHolder", () => {
            const htmlCOntent =
                '<div>Content of my message <div class="bm-corporate-signature"> ma Signature </div></div>';

            expect(
                remove(htmlCOntent, {
                    corporateSignature: buildCorporate({ usePlaceholder: true }),
                    disclaimer: null
                })
            ).toEqual("<div>Content of my message --X-BM-SIGNATURE--</div>");
        });
        it("should remove disclaimer from content", () => {
            const htmlCOntent =
                '<div>Content of my message<div class="bm-corporate-signature"> ma Signature </div><div class="bm-disclaimer">---DISCLAIMER---</div></div>';

            expect(
                remove(htmlCOntent, {
                    corporateSignature: buildCorporate(),
                    disclaimer: buildDisclaimer()
                })
            ).toEqual("<div>Content of my message</div>");
        });
    });
});

type signatureBuilder = (options?: Partial<Omit<ISignature, "isDisclaimer">>) => ISignature;

const buildCorporate: signatureBuilder = (options = {}) => {
    return {
        isDisclaimer: false,
        html: "",
        uid: "",
        text: "",
        usePlaceholder: false,
        ...options
    };
};

const buildDisclaimer: signatureBuilder = () => {
    return { isDisclaimer: true, html: "", uid: "", text: "", usePlaceholder: false };
};
