import sanitizeHtml from "../src/sanitizeHtml";
import { preventStyleInvading, WRAPPER_ID, computeNewSelector, getStyleRules } from "../src/sanitizeHtml";

describe("Sanitize HTML using the 'xss' library", () => {
    /** We want to keep more tags than those allowed by the 'xss' library. */
    test("Additional tags are kept", () => {
        const additionalTags = ["html", "body", "head", "style", "button", "table", "resourcetemplate"];
        additionalTags.forEach(tag => {
            const input = "<" + tag + ">inner</" + tag + ">";
            expect(sanitizeHtml(input, true)).toEqual(input);
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
            expect(sanitizeHtml(input, true)).toEqual(input);
        });
    });
    /** We do more filtering than the 'xss' library to links URLs.  */
    test("Link having no protocol is rejected", () => {
        const url = '<a href="ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(url, true)).toEqual("<a>linkDisplay</a>");
    });
    test("Link having a forbidden protocol is rejected", () => {
        const url = '<a href="proctocol://ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(url, true)).toEqual("<a>linkDisplay</a>");
    });
    test("Link having an allowed protocol is kept", () => {
        const url = '<a href="https://ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(url, true)).toEqual(url);
    });
    test("Link having an allowed protocol but a wrong case is fixed", () => {
        const url = '<a href="hTtpS://ta/ta/yoyo">linkDisplay</a>';
        const fixedUrl = '<a href="https://ta/ta/yoyo">linkDisplay</a>';
        expect(sanitizeHtml(url, true)).toEqual(fixedUrl);
    });
    test("Image with blob source should be allowed", () => {
        const url = '<img src="blob:https://webmail-test.loc/8aa75f30-e3e2-4d70-89ba-a8062b762b3e" />';
        expect(sanitizeHtml(url, true)).toEqual(url);
    });
});

describe("Prevent style invading", () => {
    const wrapperSelector = "#" + WRAPPER_ID;

    test("head and body styles are parsed to prevent style invading", () => {
        const headCssRule = " p {background-color: red;}";
        const cssRule = " .maClasse {top: 0;}";

        const html = `
            <html>
                <head>
                    <style>${headCssRule}</style>
                </head>
                <body><style>${cssRule}</style></body>
            </html>`;

        const expected =
            `<div id="${WRAPPER_ID}">
            <style>\n` +
            wrapperSelector +
            headCssRule +
            "\n" +
            wrapperSelector +
            cssRule +
            "</style></div>";
        expect(preventStyleInvading(html)).toBe(expected);
    });

    test("classic selectors", () => {
        expect(computeNewSelector(".maClasse")).toBe(wrapperSelector + " .maClasse");

        expect(computeNewSelector(".maClasse, .anotherClass")).toBe(
            wrapperSelector + " .maClasse," + wrapperSelector + " .anotherClass"
        );
        expect(computeNewSelector(".maClasse.anotherClass")).toBe(wrapperSelector + " .maClasse.anotherClass");
        expect(computeNewSelector(".maClasse .anotherClass")).toBe(wrapperSelector + " .maClasse .anotherClass");

        expect(computeNewSelector("div")).toBe(wrapperSelector + " div");

        expect(computeNewSelector("img[src='truc']")).toBe(wrapperSelector + " img[src='truc']");

        expect(computeNewSelector("*")).toBe(wrapperSelector + " *");
    });

    test("selectors containg 'body' or 'html'", () => {
        expect(computeNewSelector("body")).toBe(wrapperSelector + " ");
        expect(computeNewSelector("html")).toBe(wrapperSelector + " ");

        expect(computeNewSelector(".bodyBuilder")).toBe(wrapperSelector + " .bodyBuilder");
        expect(computeNewSelector(".htmlBuilder")).toBe(wrapperSelector + " .htmlBuilder");

        expect(computeNewSelector("body .builder")).toBe(wrapperSelector + " .builder");
        expect(computeNewSelector("html .builder")).toBe(wrapperSelector + " .builder");

        expect(computeNewSelector("body.builder")).toBe(wrapperSelector + " ");
        expect(computeNewSelector("html.builder")).toBe(wrapperSelector + " ");

        expect(computeNewSelector("html.machin > body")).toBe(wrapperSelector + " ");
        expect(computeNewSelector("html.machin>.maClasse")).toBe(wrapperSelector + " >.maClasse");

        expect(computeNewSelector("body, .maClasse, body > .anotherClass")).toBe(
            wrapperSelector + " ," + wrapperSelector + " .maClasse," + wrapperSelector + " > .anotherClass"
        );
    });

    test("@media and @font-face rules are not preserved", () => {
        const cssRules = `<style>
            @media screen {
                @font-face {
                font-family: "Lato";
                font-style: normal;
                font-weight: 400;
                src: local("Lato Regular"), local("Lato-Regular"), url(https://fonts.gstatic.com/s/lato/v11/qIIYRU-oROkIk8vfvxw6QvesZW2xOQ-xsNqO47m55DA.woff) format("woff");
                }
            
                @font-face {
                font-family: "Lato";
                font-style: normal;
                font-weight: 700;
                src: local("Lato Bold"), local("Lato-Bold"), url(https://fonts.gstatic.com/s/lato/v11/qdgUG4U09HnJwhYI-uK18wLUuEpTyoUstqEm5AMlJo4.woff) format("woff");
                }
            
                @font-face {
                font-family: "Lato";
                font-style: italic;
                font-weight: 400;
                src: local("Lato Italic"), local("Lato-Italic"), url(https://fonts.gstatic.com/s/lato/v11/RYyZNoeFgb0l7W3Vu1aSWOvvDin1pK8aKteLpeZ5c0A.woff) format("woff");
                }
            
                @font-face {
                font-family: 'Lato';
                font-style: normal;
                font-weight: 900;
                src: local('Lato Black'), local('Lato-Black'), url(https://fonts.gstatic.com/s/lato/v14/S6u9w4BMUTPHh50XSwiPGQ3q5d0.woff2) format('woff2');
                }
            }
        </style>`;
        const html =
            `
            <html>
                <head>` +
            cssRules +
            `</head>
                <body><style> p { color: red;}</style>` +
            cssRules +
            `</body>
            </html>
        `;
        const doc = new DOMParser().parseFromString(html, "text/html");
        const result = getStyleRules(doc);
        expect(result).not.toContain(cssRules);
        expect(result).toBe("\n" + wrapperSelector + " p {color: red;}");
    });
});
