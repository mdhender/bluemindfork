import InlineImageHelper from "../src/InlineImageHelper";

describe("InlineImageHelper insertInlineImages", () => {
    test("standard case", async () => {
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
            "2.3": "45D67E89\n1A23BC6\n",
            "2.4": imagePart2Content
        };
        const htmlWithCid = `
                <html>
                    <body>
                        This is my referenced image:<img alt="myImage" src="cid:123456789@test.com">
                    </body>
                </html>`;

        const expectedContent =
            `
                <html>
                    <body>
                        This is my referenced image:<img alt="myImage" src="` +
            imagePart2Content +
            `" data-bm-cid="<123456789@test.com>">
                    </body>
                </html>`;

        const result = await InlineImageHelper.insertAsBase64([htmlWithCid], imageParts, partContentByAddress);

        expect(result.contentsWithImageInserted[0]).toEqual(expectedContent);
        expect(result.imageInlined.length).toEqual(1);
        expect(result.imageInlined[0].address).toEqual("2.4");
    });

    test("a more complex case", async () => {
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

        const htmlWithCid = `<html>
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
        </html>`;

        const partContentByAddress = {
            "3.4": imagePart2Content,
            "3.6": imagePart1Content
        };

        const expectedContent =
            `<html>
            <body>
                <p><img src="` +
            imagePart1Content +
            `" data-bm-cid="<999999999@test.com>"></p>
                <img src="http://... " />
                Salut Kévin j'ai un src="cid:123456789@test.com" dans mon mail c'est normal ?
                <div>
                    <img alt="alternative text" width="42px" 
                        src="` +
            imagePart2Content +
            `" data-bm-cid="<123456789@test.com>" height="42px">
                </div>
                <p><img src="cid:DOESNOTEXIST"></p>
            </body>
        </html>`;

        const result = await InlineImageHelper.insertAsBase64([htmlWithCid], imageParts, partContentByAddress);

        expect(result.contentsWithImageInserted[0]).toEqual(expectedContent);
        expect(result.imageInlined.length).toEqual(2);
        expect(result.imageInlined.map(part => part.address)).toEqual(["3.6", "3.4"]);
    });
});
