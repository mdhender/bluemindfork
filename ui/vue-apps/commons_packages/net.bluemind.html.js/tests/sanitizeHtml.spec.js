import sanitizeHtml from "../src/sanitizeHtml";

describe("Sanitize HTML using the 'xss' library", () => {
    /** We want to keep more tags than those allowed by the 'xss' library. */
    test("Additional tags are kept", () => {
        const additionalTags = ["html", "body", "head", "style", "button", "table", "resourcetemplate"];
        additionalTags.forEach(tag => {
            const input = "<" + tag + ">inner</" + tag + ">";
            expect(sanitizeHtml(input)).toEqual(input);
        });
    });
    /** We want to keep more attributes than those allowed by the 'xss' library. */
    test("Additional attributes are kept", () => {
        const additionalAttributes = [
            "class",
            "type",
            "style",
            "id",
            "height",
            "width",
            "border",
            "bgcolor",
            "leftmargin",
            "topmargin",
            "marginwidth",
            "marginheight"
        ];
        additionalAttributes.forEach(attribute => {
            const input = "<div " + attribute + '="someValue">inner</div>';
            expect(sanitizeHtml(input)).toEqual(input);
        });
    });
    /** We do more filtering than the 'xss' library to links URLs.  */
    test("Link having no protocol is rejected", () => {
        const html = '<a href="ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(html)).toEqual("<a>linkDisplay</a>");
    });
    test("Link having a forbidden protocol is rejected", () => {
        const html = '<a href="proctocol://ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(html)).toEqual("<a>linkDisplay</a>");
    });
    test("Link having an allowed protocol is kept", () => {
        const html = '<a href="https://ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(html)).toEqual(html);
    });
    test("Link having an allowed protocol but a wrong case is fixed", () => {
        const html = '<a href="hTtpS://ta/ta/yoyo">linkDisplay</a>';
        const fixedHtml = '<a href="https://ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(html)).toEqual(fixedHtml);
    });
    test("Image with blob source should be allowed", () => {
        const html = '<img src="blob:https://webmail-test.loc/8aa75f30-e3e2-4d70-89ba-a8062b762b3e">';
        expect(sanitizeHtml(html)).toEqual(html);
    });
});
