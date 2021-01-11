import GetInlinePartsVisitor from "../../helpers/GetInlinePartsVisitor";
import TreeWalker from "../../helpers/TreeWalker";

describe("GetInlinePartsVisitor", () => {
    /**
     *  text/plain
     */
    test("Check Plain (just a text/plain part)", () => {
        // build the input
        const rootPart = { mime: "text/plain", children: [], address: "1" };

        // build the expected result
        const expected = [{ capabilities: [], parts: [rootPart] }];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     * multipart/alternative
     *  |
     *  ---text/plain
     *  |
     *  ---text/html
     */
    test("Check Alternative[Plain, Html]", () => {
        // build the input
        const plainPart = { mime: "text/plain", address: "1" };
        const htmlPart = { mime: "text/html", address: "2" };
        const rootPart = { mime: "multipart/alternative", children: [plainPart, htmlPart], address: "TEXT" };

        // build the expected result
        const expected = [
            { capabilities: ["text/html"], parts: [htmlPart] },
            { capabilities: ["text/plain"], parts: [plainPart] }
        ];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     * multipart/related
     *  |
     *  ---text/html
     *  |
     *  ---image/png
     *  |
     *  ---image/png
     *  |
     *  ---image/png
     */
    test("Check Related[Html, Image, Image, Image]", () => {
        // build the input
        const htmlPart = { mime: "text/html", address: "1" };
        const image1Part = { mime: "image/png", address: "2" };
        const image2Part = { mime: "image/png", address: "3" };
        const image3Part = { mime: "image/png", address: "4" };
        const rootPart = {
            mime: "multipart/related",
            children: [htmlPart, image1Part, image2Part, image3Part],
            address: "TEXT"
        };

        // build the expected result
        const expected = [{ capabilities: [], parts: [htmlPart, image1Part, image2Part, image3Part] }];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     * multipart/mixed
     *  |
     *  ---text/plain
     *  |
     *  ---image/png
     *  |
     *  ---text/plain
     */
    test("Check Mixed[Plain, Image, Plain]", () => {
        // build the input
        const plainPart1 = { mime: "text/plain", address: "1" };
        const imagePart = { mime: "image/png", address: "2" };
        const plainPart2 = { mime: "text/plain", address: "3" };
        const rootPart = { mime: "multipart/mixed", children: [plainPart1, imagePart, plainPart2], address: "TEXT" };

        // build the expected result
        const expected = [{ capabilities: [], parts: [plainPart1, imagePart, plainPart2] }];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     * multipart/alternative
     *  |
     *  ---text/plain
     *  |
     *  ---multipart/related
     *      |
     *      ---text/html
     *      |
     *      ---image/png
     */
    test("Check Alternative[Plain, Related[Html, Image]]", () => {
        // build the input
        const plainPart = { mime: "text/plain", address: "1" };
        const htmlPart = { mime: "text/html", address: "2.1" };
        const imagePart = { mime: "image/png", address: "2.2" };
        const relPart = { mime: "multipart/related", children: [htmlPart, imagePart], address: "2" };
        const rootPart = { mime: "multipart/alternative", children: [plainPart, relPart], address: "TEXT" };

        // build the expected result
        const expected = [
            { capabilities: ["text/html"], parts: [htmlPart, imagePart] },
            { capabilities: ["text/plain"], parts: [plainPart] }
        ];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     * multipart/related
     *  |
     *  ---multipart/alternative
     *  |   |
     *      ---text/plain
     *  |   |
     *  |   ---text/html
     *  |
     *  ---image/png
     *  |
     *  ---image/png
     *  |
     *  ---image/png
     */
    test("Check Related[Alternative[Plain, Html], Image, Image, Image]", () => {
        // build the input
        const plainPart = { mime: "text/plain", address: "1.1" };
        const htmlPart = { mime: "text/html", address: "1.2" };
        const altPart = { mime: "multipart/alternative", children: [plainPart, htmlPart], address: "1" };
        const image1Part = { mime: "image/png", address: "2" };
        const image2Part = { mime: "image/png", address: "3" };
        const image3Part = { mime: "image/png", address: "4" };
        const rootPart = {
            mime: "multipart/related",
            children: [altPart, image1Part, image2Part, image3Part],
            address: "TEXT"
        };

        // build the expected result
        const expected = [
            { capabilities: ["text/html"], parts: [htmlPart, image1Part, image2Part, image3Part] },
            { capabilities: ["text/plain"], parts: [plainPart, image1Part, image2Part, image3Part] }
        ];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     * multipart/mixed
     *  |
     *  ---multipart/related
     *      |
     *      ---multipart/alternative
     *      |   |
     *      |   ---text/plain
     *      |   |
     *      |   ---text/html
     *      |
     *      ---image/png
     */
    test("Check Mixed[Related[Alternative[Plain, Html], Image]]", () => {
        // build the input
        const plainPart = { mime: "text/plain", address: "1.1.1" };
        const htmlPart = { mime: "text/html", address: "1.1.2" };
        const altPart = { mime: "multipart/alternative", children: [plainPart, htmlPart], address: "1.1" };
        const imagePart = { mime: "image/png", address: "1.2" };
        const relPart = { mime: "multipart/related", children: [altPart, imagePart], address: "1" };
        const rootPart = { mime: "multipart/mixed", children: [relPart], address: "TEXT" };

        // build the expected result
        const expected = [
            { capabilities: ["text/html"], parts: [htmlPart, imagePart] },
            { capabilities: ["text/plain"], parts: [plainPart, imagePart] }
        ];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     * multipart/alternative
     *  |
     *  ---text/plain
     *  |
     *  ---multipart/mixed
     *      |
     *      ---text/html
     *      |
     *      ---multipart/alternative
     *          |
     *          ---text/plain
     *          |
     *          ---application/pdf
     */
    test("Check Alternative[Plain, Mixed[Html, Alternative[Plain, Pdf]]]", () => {
        // build the input
        const plainPart1 = { mime: "text/plain", address: "1" };
        const htmlPart = { mime: "text/html", address: "2.1" };
        const plainPart2 = { mime: "text/plain", address: "2.2.1" };
        const pdfPart = { mime: "application/pdf", address: "2.2.2" };
        const altPart = { mime: "multipart/alternative", children: [plainPart2, pdfPart], address: "2.2" };
        const mixedPart = { mime: "multipart/mixed", children: [htmlPart, altPart], address: "2" };
        const rootPart = { mime: "multipart/alternative", children: [plainPart1, mixedPart], address: "TEXT" };

        // build the expected result
        const expected = [
            { capabilities: ["text/html", "application/pdf"], parts: [htmlPart, pdfPart] },
            { capabilities: ["text/html", "text/plain"], parts: [htmlPart, plainPart2] },
            { capabilities: ["text/plain"], parts: [plainPart1] }
        ];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     * multipart/alternative
     *  |
     *  ---multipart/mixed
     *  |   |
     *  |   ---text/plain
     *  |   |
     *  |   ---multipart/alternative
     *  |       |
     *  |       ---text/plain
     *  |       |
     *  |       ---application/pdf
     *  |
     *  ---text/html
     */
    test("Check Alternative[Mixed[Plain, Alternative[Plain, Pdf]], Html]", () => {
        // build the input
        const plainPart1 = { mime: "text/plain", address: "2.1" };
        const htmlPart = { mime: "text/html", address: "2" };
        const plainPart2 = { mime: "text/plain", address: "1.2.1" };
        const pdfPart = { mime: "application/pdf", address: "1.2.2" };
        const altPart = { mime: "multipart/alternative", children: [plainPart2, pdfPart], address: "1.2" };
        const mixedPart = { mime: "multipart/mixed", children: [plainPart1, altPart], address: "1" };
        const rootPart = { mime: "multipart/alternative", children: [mixedPart, htmlPart], address: "TEXT" };

        // build the expected result
        const expected = [
            { capabilities: ["text/html"], parts: [htmlPart] },
            { capabilities: ["text/plain", "application/pdf"], parts: [plainPart1, pdfPart] },
            { capabilities: ["text/plain"], parts: [plainPart1, plainPart2] }
        ];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     * multipart/alternative
     *  |
     *  ---text/plain
     *  |
     *  ---multipart/mixed
     *      |
     *      ---text/html
     *      |
     *      ---multipart/alternative
     *      |   |
     *      |   ---text/plain
     *      |   |
     *      |   ---application/pdf
     *      |
     *      ---image/png
     */
    test("Check Alternative[Plain, Mixed[Html, Alternative[Plain, Pfd], Image]]", () => {
        // build the input
        const plainPart1 = { mime: "text/plain", address: "1" };
        const htmlPart = { mime: "text/html", address: "2.1" };
        const plainPart2 = { mime: "text/plain", address: "2.1.1" };
        const pdfPart = { mime: "application/pdf", address: "2.1.2" };
        const imagePart = { mime: "image/png", address: "2.3" };
        const altPart = { mime: "multipart/alternative", children: [plainPart2, pdfPart], address: "2.2" };
        const mixedPart = { mime: "multipart/mixed", children: [htmlPart, altPart, imagePart], address: "2" };
        const rootPart = { mime: "multipart/alternative", children: [plainPart1, mixedPart], address: "TEXT" };

        // build the expected result
        const expected = [
            { capabilities: ["text/html", "application/pdf", "image/png"], parts: [htmlPart, pdfPart, imagePart] },
            { capabilities: ["text/html", "text/plain", "image/png"], parts: [htmlPart, plainPart2, imagePart] },
            { capabilities: ["text/plain"], parts: [plainPart1] }
        ];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     * multipart/mixed
     *  |
     *  ---multipart/related
     *  |   |
     *  |   ---multipart/alternative
     *  |   |   |
     *  |   |   ---text/plain
     *  |   |   |
     *  |   |   ---text/html
     *  |   |
     *  |   ---image/png
     *  |
     *  ---multipart/related
     *      |
     *      ---multipart/alternative
     *      |   |
     *      |   ---text/plain
     *      |   |
     *      |   ---text/html
     *      |
     *      ---image/png
     */
    test("Check Mixed[Related[Alternative[Plain, Html], Image], Related[Alternative[Plain, Html], Image]]", () => {
        // build the input
        const plainPart1 = { mime: "text/plain", address: "1.1.1" };
        const htmlPart1 = { mime: "text/html", address: "1.1.2" };
        const altPart1 = { mime: "multipart/alternative", children: [plainPart1, htmlPart1], address: "1.1" };
        const plainPart2 = { mime: "text/plain", address: "2.1.1" };
        const htmlPart2 = { mime: "text/html", address: "2.1.2" };
        const altPart2 = { mime: "multipart/alternative", children: [plainPart2, htmlPart2], address: "2.1" };
        const imagePart1 = { mime: "image/png", address: "1.2" };
        const imagePart2 = { mime: "image/png", address: "2.2" };
        const relPart1 = { mime: "multipart/related", children: [altPart1, imagePart1], address: "1" };
        const relPart2 = { mime: "multipart/related", children: [altPart2, imagePart2], address: "2" };
        const rootPart = { mime: "multipart/mixed", children: [relPart1, relPart2], address: "TEXT" };

        // build the expected result
        const expected = [
            { capabilities: ["text/html"], parts: [htmlPart1, imagePart1, htmlPart2, imagePart2] },
            { capabilities: ["text/plain"], parts: [plainPart1, imagePart1, plainPart2, imagePart2] }
        ];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });

    /**
     *  Test message with an alternative part without children (https://forge.bluemind.net/jira/browse/FEATWEBML-1421)
     */
    test("BM-1421 : Alternative part without children", () => {
        // build the input
        const rootPart = {
            mime: "multipart/mixed",
            address: "TEXT",
            children: [
                { mime: "multipart/alternative", address: "1" },
                { mime: "text/plain", address: "2" }
            ],
            size: 26903
        };

        // build the expected result
        const expected = [{ capabilities: [], parts: [{ mime: "text/plain", address: "2" }] }];

        // run the code
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, [visitor]);
        walker.walk();
        const result = visitor.result();

        // compare the actual result and the expected one
        expect(result).toEqual(expected);
    });
});
