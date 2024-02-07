import signatureUtils from "../signature";
import { LineElement } from "../signature/LineElement";
describe("Signature", () => {
    `<div style="font-family: &quot;Helvetica Neue&quot;, Helvetica, &quot;Nimbus Sans&quot;, Arial, sans-serif; font-size: 9.75pt; color: rgb(83, 83, 83);">UNE SIGNATURE&nbsp;</div>
    <div style="font-family: &quot;Helvetica Neue&quot;, Helvetica, &quot;Nimbus Sans&quot;, Arial, sans-serif; font-size: 9.75pt; color: rgb(83, 83, 83);"><span style="font-size: 18pt; line-height: normal; color: rgb(220, 53, 69);"><span style="line-height: normal;"><i style="">QUI SE BALADAIT&nbsp;</i></span></span></div><div style="font-family: &quot;Helvetica Neue&quot;, Helvetica, &quot;Nimbus Sans&quot;, Arial, sans-serif; font-size: 9.75pt; color: rgb(83, 83, 83);"><span style="font-size: 18pt; line-height: normal; color: rgb(220, 53, 69);"><span style="line-height: normal;"><i style=""><span style="font-family: &quot;Helvetica Neue&quot;, Helvetica, &quot;Nimbus Sans&quot;, Arial, sans-serif; font-size: 18pt; color: rgb(0, 0, 0); background-color: rgb(255, 199, 0);">AU bord d'un <strike>courriel</strike></span><br></i></span></span></div>`;
    describe("Empty signature line", () => {
        test("One line signature", () => {
            expect(new LineElement(domParseFromString(`<div>-=Awesome=-</div>`)).isEmpty).toBeFalsy();
        });
        test("signature wrapped in container", () => {
            let signatureLine = `<div 
            style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span
                    style="color: rgb(10, 42, 134);"><i>-=Vincent=-</i></span><br></div>`;

            expect(new LineElement(domParseFromString(signatureLine)).isEmpty).toBeFalsy();
        });
        it("does not consider white space as valid content", () => {
            const signatureLine = `<div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span
                style="color: rgb(10, 42, 134);"><i>     </i></span><br></div>`;

            expect(new LineElement(domParseFromString(signatureLine)).isEmpty).toBeTruthy();
        });
    });

    function domParseFromString(stringContent) {
        return new DOMParser().parseFromString(stringContent, "text/html")?.body.childNodes[0];
    }

    describe("Trim signature leading and trailing empty html elements", () => {
        test("Basic signature", () => {
            let signature = `
  
        <div><br></div>
        <div><style></style></div>
        <div><i><b><br></b></i></div>

        <div style="height: 20px; background-image: url('http://fakeurl.bluemind.net/image.png')"></div>
        <div>-=My=-</div>
        <div><br></div>
        <div><br></div>
        <div><style></style></div>
        <div>-=Awesome=-</div>
        <div><br></div>
        <div>-=Signature=-</div>
        <div><p><img src="http://fakeurl.bluemind.net/image.png"></p></div>
  
        <div><style></style></div>

        <div><br></div>
`;
            let trimmed = `<div style="height: 20px; background-image: url('http://fakeurl.bluemind.net/image.png')"></div>
        <div>-=My=-</div>
        <div><br></div>
        <div><br></div>
        <div><style></style></div>
        <div>-=Awesome=-</div>
        <div><br></div>
        <div>-=Signature=-</div>
        <div><p><img src="http://fakeurl.bluemind.net/image.png"></p></div>`;

            expect(signatureUtils.trimSignature(signature)).toEqual(trimmed);
        });

        test("over-wrapped signature", () => {
            const signature = `
        <div id="bm-composer-content-wrapper">
            <div id="bm-composer-content-wrapper">
                <div id="bm-composer-content-wrapper" style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);">
                    <span style="color: rgb(10, 42, 134);"><i><br></i></span>
                </div>
                <div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);">
                    <span style="color: rgb(10, 42, 134);"><i><br></i></span>
                </div>
                <div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span
                    style="color: rgb(10, 42, 134);"><i>-=Vincent=-</i></span><br></div>
                <div>
            </div>

            <div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span
                style="color: rgb(10, 42, 134);"><i><br></i></span>
            </div>
        </div>`;

            const trimmed = `<div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span style="color: rgb(10, 42, 134);"><i>-=Vincent=-</i></span><br></div>`;

            expect(signatureUtils.trimSignature(signature)).toEqual(trimmed);
        });

        it("should remove rooster.js container  ", () => {
            const signature = `<div class="roosterjs-container flex-fill overflow-auto" style="user-select: text;" contenteditable="true">
                <div id="bm-composer-content-wrapper"><div id="bm-composer-content-wrapper"><p style="margin-bottom: 0; margin-top: 0;">--<br></p><p style="margin-bottom: 0; margin-top: 0;">Pierre Baudracco / 06 82 84 63 67 / @pierrebod<br>BlueMind - www.bluemind.net - <a href="https://www.bluemind.net/breves/nouvelle-version-bluemind-4-0/" target="_blank">La seule messagerie compatible Outlook sans connecteur</a><br>Co-président du CNLL, vice-président du Hub Open Source de Systematic, président de SoLibre<br></p></div><style></style></div></div>`;

            const trimmed = `<div id="bm-composer-content-wrapper"><p style="margin-bottom: 0; margin-top: 0;">--<br></p><p style="margin-bottom: 0; margin-top: 0;">Pierre Baudracco / 06 82 84 63 67 / @pierrebod<br>BlueMind - www.bluemind.net - <a href="https://www.bluemind.net/breves/nouvelle-version-bluemind-4-0/" target="_blank">La seule messagerie compatible Outlook sans connecteur</a><br>Co-président du CNLL, vice-président du Hub Open Source de Systematic, président de SoLibre<br></p></div>`;

            expect(signatureUtils.trimSignature(signature)).toEqual(trimmed);
        });

        it("should Keep HtmlElement with background-image", () => {
            expect(
                signatureUtils.trimSignature(
                    `<div style="color:red;"><br></div>
                    <div style="height: 20px; background-image: url('http://fakeurl.bluemind.net/image.png')"></div>
                    <div><br></div>`
                )
            ).toEqual(
                `<div style="height: 20px; background-image: url('http://fakeurl.bluemind.net/image.png')"></div>`
            );
        });

        describe("Trim Text Content Signature", () => {
            test("trim leading empty space ", () => {
                const signature = "      UNE SINGAUTRE\n    EN TEXT MAIS\n          EN 3 LIGNES";
                expect(signatureUtils.trimSignature(signature)).toEqual(
                    "UNE SINGAUTRE\n    EN TEXT MAIS\n          EN 3 LIGNES"
                );
            });
        });
    });
});
