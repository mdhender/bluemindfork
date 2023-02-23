import signatureUtils from "../signature";

describe("Signature", () => {
    beforeAll(() => {
        // since our DOM is not rendered, functions like HTMElement.innerText will return nothing
        signatureUtils.setRenderless(true);
    });
    afterAll(() => {
        signatureUtils.setRenderless(false);
    });
    test("Empty signature line", () => {
        let signatureLine = `<div>-=Awesome=-</div>`;
        let node = new DOMParser().parseFromString(signatureLine, "text/html")?.body.childNodes[0];
        expect(signatureUtils.isSignatureLineEmpty(node)).toBeFalsy();

        signatureLine = `<div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span
        style="color: rgb(10, 42, 134);"><i>-=Vincent=-</i></span><br></div>`;
        node = new DOMParser().parseFromString(signatureLine, "text/html")?.body.childNodes[0];
        expect(signatureUtils.isSignatureLineEmpty(node)).toBeFalsy();

        signatureLine = `<div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span
        style="color: rgb(10, 42, 134);"><i>  </i></span><br></div>`;
        node = new DOMParser().parseFromString(signatureLine, "text/html")?.body.childNodes[0];
        expect(signatureUtils.isSignatureLineEmpty(node)).toBeTruthy();
    });
    test("Trim signature", () => {
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

        signature = `
    <div id="bm-composer-content-wrapper">
        <div id="bm-composer-content-wrapper">
            <div id="bm-composer-content-wrapper"
                style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span
                    style="color: rgb(10, 42, 134);"><i><br></i></span></div>
            <div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span
                    style="color: rgb(10, 42, 134);"><i><br></i></span></div>
            <div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span
                    style="color: rgb(10, 42, 134);"><i>-=Vincent=-</i></span><br></div>
            <div></div>
            
            <div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span
                    style="color: rgb(10, 42, 134);"><i><br></i></span></div>
        </div>
    </div>`;

        trimmed = `<div style="font-family: &quot;Montserrat&quot;, sans-serif; font-size: 9pt; color: rgb(31, 31, 31);"><span style="color: rgb(10, 42, 134);"><i>-=Vincent=-</i></span><br></div>`;

        expect(signatureUtils.trimSignature(signature)).toEqual(trimmed);

        signature = `<div class="roosterjs-container flex-fill overflow-auto" style="user-select: text;" contenteditable="true"><div id="bm-composer-content-wrapper"><div id="bm-composer-content-wrapper"><p style="margin-bottom: 0; margin-top: 0;">--<br></p><p style="margin-bottom: 0; margin-top: 0;">Pierre Baudracco / 06 82 84 63 67 / @pierrebod<br>BlueMind - www.bluemind.net - <a href="https://www.bluemind.net/breves/nouvelle-version-bluemind-4-0/" target="_blank">La seule messagerie compatible Outlook sans connecteur</a><br>Co-président du CNLL, vice-président du Hub Open Source de Systematic, président de SoLibre<br></p></div><style></style></div></div>`;

        trimmed = `<div id="bm-composer-content-wrapper"><p style="margin-bottom: 0; margin-top: 0;">--<br></p><p style="margin-bottom: 0; margin-top: 0;">Pierre Baudracco / 06 82 84 63 67 / @pierrebod<br>BlueMind - www.bluemind.net - <a href="https://www.bluemind.net/breves/nouvelle-version-bluemind-4-0/" target="_blank">La seule messagerie compatible Outlook sans connecteur</a><br>Co-président du CNLL, vice-président du Hub Open Source de Systematic, président de SoLibre<br></p></div>`;

        expect(signatureUtils.trimSignature(signature)).toEqual(trimmed);
    });
});
