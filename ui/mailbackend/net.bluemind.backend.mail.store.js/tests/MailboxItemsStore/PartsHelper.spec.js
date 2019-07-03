import PartsHelper from "../../src/MailboxItemsStore/PartsHelper.js";

describe("PartsHelper insertInlineImages", () => {
    test("standard case", () => {
        // build input
        const partsWithReferences = [
            {
                content: `
                <html>
                    <body>
                        This is my referenced image:<img alt="myImage" src="cid:123456789@test.com">
                    </body>
                </html>`
            }
        ];
        const imagePart1 = { cid: "<999999999@test.com>", content: "45D67E89\n1A23BC6\n", mime: "image/jpg" };
        const imagePart2 = { cid: "<123456789@test.com>", content: "1A23BC6\n45D67E89\n", mime: "image/png" };
        const imageParts = [imagePart1, imagePart2];

        // build expected result
        const expected = [
            {
                content: `
                <html>
                    <body>
                        This is my referenced image:<img alt="myImage" src="data:image/png;base64, 1A23BC645D67E89">
                    </body>
                </html>`
            }
        ];

        // run the code
        PartsHelper.insertInlineImages(partsWithReferences, imageParts);

        // compare the actual result and the expected one
        expect(partsWithReferences).toEqual(expected);
    });

    test("a more complex case", () => {
        // build input
        const partsWithReferences = [
            {
                content: `
                <html>
                    <body>
                        <p><img src="cid:999999999@test.com"></p>
                        <img src="http://... " />
                        Salut Kévin j'ai un src="cid:123456789@test.com" dans mon mail c'est normal ?
                        <div>
                            <img alt="alternative text" width="42px" 
                                src="cid:123456789@test.com" height="42px">
                        </div>
                        <p><img src="cid:DOESNOTEXIST"></p>
                    </body>
                </html>`
            }
        ];
        const imagePart1 = { cid: "<999999999@test.com>", content: "45D67E89\n1A23BC6\n", mime: "image/jpg" };
        const imagePart2 = { cid: "<123456789@test.com>", content: "1A23BC6\n45D67E89\n", mime: "image/png" };
        const imageParts = [imagePart1, imagePart2];

        // build expected result
        const expected = [
            {
                content: `
                <html>
                    <body>
                        <p><img src="data:image/jpg;base64, 45D67E891A23BC6"></p>
                        <img src="http://... " />
                        Salut Kévin j'ai un src="cid:123456789@test.com" dans mon mail c'est normal ?
                        <div>
                            <img alt="alternative text" width="42px" 
                                src="data:image/png;base64, 1A23BC645D67E89" height="42px">
                        </div>
                        <p><img src="cid:DOESNOTEXIST"></p>
                    </body>
                </html>`
            }
        ];

        // run the code
        PartsHelper.insertInlineImages(partsWithReferences, imageParts);

        // compare the actual result and the expected one
        expect(partsWithReferences).toEqual(expected);
    });
});
