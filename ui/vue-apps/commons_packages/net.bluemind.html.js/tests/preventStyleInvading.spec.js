import {
    default as preventStyleInvading,
    WRAPPER_CLASS,
    computeNewSelector,
    getStyleRules
} from "../src/preventStyleInvading";

describe("Prevent style invading", () => {
    const wrapperSelector = "." + WRAPPER_CLASS;

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
            `<div class="${WRAPPER_CLASS}">
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
          
          p {
                font-family: "Lato";
                font-style: normal;
                font-weight: 400;
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

    /**
     * @jest-environment jsdom
     */
    test("DOMParser getStyleSheets", () => {
        const cssRules = `<style>
            
            div {
                font-family: "Lato";
                font-style: normal;
                font-weight: 400;
                }
            
            a {
                color: blue;
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
        expect(result).toEqual(
            `
.bm-composer-content-wrapper div {font-family: "Lato"; font-style: normal; font-weight: 400;}
.bm-composer-content-wrapper p {color: red;}`
        );
    });
});
