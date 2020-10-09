import InlineImageHelper from "../src/InlineImageHelper";

describe("InlineImageHelper insertInlineImages", () => {
    const mockedBlobAsURLObject = "blob:mockedURLObject:";
    global.URL.createObjectURL = jest.fn().mockImplementation(arg => {
        return mockedBlobAsURLObject + arg;
    });

    test("standard case", () => {
        const partsWithReferences = [{ address: "1" }];

        const imagePart1 = {
            contentId: "<999999999@test.com>",
            mime: "image/jpg",
            address: "2.3"
        };
        const imagePart2Content = "1A23BC6\n45D67E89\n";
        const imagePart2 = {
            contentId: "<123456789@test.com>",
            mime: "image/png",
            address: "2.4"
        };

        const imageParts = [imagePart1, imagePart2];

        const partContentByAddress = {
            "1": `
                <html>
                    <body>
                        This is my referenced image:<img alt="myImage" src="cid:123456789@test.com">
                    </body>
                </html>`,
            "2.3": "45D67E89\n1A23BC6\n",
            "2.4": imagePart2Content
        };

        const expectedSrcContent = mockedBlobAsURLObject + imagePart2Content;
        const expectedContent =
            `
                <html>
                    <body>
                        This is my referenced image:<img alt="myImage" src="` +
            expectedSrcContent +
            `" data-bm-imap-address="2.4">
                    </body>
                </html>`;

        InlineImageHelper.insertInlineImages(partsWithReferences, imageParts, partContentByAddress);

        expect(partContentByAddress["1"]).toEqual(expectedContent);
    });

    test("a more complex case", () => {
        const partsWithReferences = [{ address: "1" }];

        const imagePart1Content = "45D67E89\n1A23BC6\n";
        const imagePart1 = {
            contentId: "<999999999@test.com>",
            mime: "image/jpg",
            address: "3.6"
        };
        const imagePart2Content = "1A23BC6\n45D67E89\n";
        const imagePart2 = {
            contentId: "<123456789@test.com>",
            mime: "image/png",
            address: "3.4"
        };
        const imageParts = [imagePart1, imagePart2];

        const partContentByAddress = {
            "1": `
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
            </html>`,
            "3.4": imagePart2Content,
            "3.6": imagePart1Content
        };

        const expectedContent =
            `
            <html>
                <body>
                    <p><img src="` +
            mockedBlobAsURLObject +
            imagePart1Content +
            `" data-bm-imap-address="3.6"></p>
                    <img src="http://... " />
                    Salut Kévin j'ai un src="cid:123456789@test.com" dans mon mail c'est normal ?
                    <div>
                        <img alt="alternative text" width="42px" 
                            src="` +
            mockedBlobAsURLObject +
            imagePart2Content +
            `" data-bm-imap-address="3.4" height="42px">
                    </div>
                    <p><img src="cid:DOESNOTEXIST"></p>
                </body>
            </html>`;

        InlineImageHelper.insertInlineImages(partsWithReferences, imageParts, partContentByAddress);

        expect(partContentByAddress["1"]).toEqual(expectedContent);
    });
});
