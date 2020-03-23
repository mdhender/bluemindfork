import { sanitizeHtml } from "../src";

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
        const url = '<a href="ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(url)).toEqual("<a>linkDisplay</a>");
    });
    test("Link having a forbidden protocol is rejected", () => {
        const url = '<a href="proctocol://ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(url)).toEqual("<a>linkDisplay</a>");
    });
    test("Link having an allowed protocol is kept", () => {
        const url = '<a href="https://ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(url)).toEqual(url);
    });
});
