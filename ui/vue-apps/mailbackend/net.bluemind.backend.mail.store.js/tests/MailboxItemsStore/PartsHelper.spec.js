import PartsHelper from "../../src/MailboxItemsStore/PartsHelper.js";

describe("PartsHelper insertInlineImages", () => {
    const mockedBlobAsURLObject = "blob:mockedURLObject:";
    global.URL.createObjectURL = jest.fn().mockImplementation(arg => {
        return mockedBlobAsURLObject + arg;
    });

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
        const imagePart1 = { contentId: "<999999999@test.com>", content: "45D67E89\n1A23BC6\n", mime: "image/jpg" };
        const imagePart2Content = "1A23BC6\n45D67E89\n";
        const imagePart2 = { contentId: "<123456789@test.com>", content: imagePart2Content, mime: "image/png" };
        const imageParts = [imagePart1, imagePart2];

        // build expected result
        const expectedSrcContent = mockedBlobAsURLObject + imagePart2Content;
        const expected = [
            {
                content:
                    `
                <html>
                    <body>
                        This is my referenced image:<img alt="myImage" src="` +
                    expectedSrcContent +
                    `">
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
        const imagePart1Content = "45D67E89\n1A23BC6\n";
        const imagePart1 = { contentId: "<999999999@test.com>", content: imagePart1Content, mime: "image/jpg" };
        const imagePart2Content = "1A23BC6\n45D67E89\n";
        const imagePart2 = { contentId: "<123456789@test.com>", content: imagePart2Content, mime: "image/png" };
        const imageParts = [imagePart1, imagePart2];

        // build expected result
        const expected = [
            {
                content:
                    `
                <html>
                    <body>
                        <p><img src="` +
                    mockedBlobAsURLObject +
                    imagePart1Content +
                    `"></p>
                        <img src="http://... " />
                        Salut Kévin j'ai un src="cid:123456789@test.com" dans mon mail c'est normal ?
                        <div>
                            <img alt="alternative text" width="42px" 
                                src="` +
                    mockedBlobAsURLObject +
                    imagePart2Content +
                    `" height="42px">
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
