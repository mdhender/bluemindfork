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

describe("InlineImageHelper insertCid", () => {
    test("line break into base64 image", async () => {
        const html = `<table><tbody><tr><td><p style="color: #212121; font-family: Helvetica, sans-serif; font-size: 0.9em;"><a title="Twitter" href="https://twitter.com/_bluemind?lang=fr"><img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA8AAAAPCAYAAAA71pVKAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAE82lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4gPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgNS42LWMxNDIgNzkuMTYwOTI0LCAyMDE3LzA3LzEzLTAxOjA2OjM5ICAgICAgICAiPiA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPiA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgeG1sbnM6cGhvdG9zaG9wPSJodHRwOi8vbnMuYWRvYmUuY29tL3Bob3Rvc2hvcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ0MgMjAxOCAoV2luZG93cykiIHhtcDpDcmVhdGVEYXRlPSIyMDE4LTA5LTA3VDExOjU5OjE4KzAyOjAwIiB4bXA6TW9kaWZ5RGF0ZT0iMjAxOC0wOS0wN1QxMjowMDoxOCswMjowMCIgeG1wOk1ldGFkYXRhRGF0ZT0iMjAxOC0wOS0wN1QxMjowMDoxOCswMjowMCIgZGM6Zm9ybWF0PSJpbWFnZS9wbmciIHBob3Rvc2hvcDpDb2xvck1vZGU9IjMiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6OGJjZmNlMWMtMmM5Ny01MjQzLWI1ZTQtY2ZiNzM1YTRkOThmIiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOjhiY2ZjZTFjLTJjOTctNTI0My1iNWU0LWNmYjczNWE0ZDk4ZiIgeG1wTU06T3JpZ2luYWxEb2N1bWVudElEPSJ4bXAuZGlkOjhiY2ZjZTFjLTJjOTctNTI0My1iNWU0LWNmYjczNWE0ZDk4ZiI+IDx4bXBNTTpIaXN0b3J5PiA8cmRmOlNlcT4gPHJkZjpsaSBzdEV2dDphY3Rpb249ImNyZWF0ZWQiIHN0RXZ0Omluc3RhbmNlSUQ9InhtcC5paWQ6OGJjZmNlMWMtMmM5Ny01MjQzLWI1ZTQtY2ZiNzM1YTRkOThmIiBzdEV2dDp3aGVuPSIyMDE4LTA5LTA3VDExOjU5OjE4KzAyOjAwIiBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZG9iZSBQaG90b3Nob3AgQ0MgMjAxOCAoV2luZG93cykiLz4gPC9yZGY6U2VxPiA8L3htcE1NOkhpc3Rvcnk+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+n7KHaAAAAjZJREFUKJFtkz9Pk2EUxX/3eZ+XFkoLiKSJQAzRGB0IhJKICwPGxUlC1IQQNx2ddPETGGP8BG7gKOjgIJvK5oBoBEFNE1GrjS3hT1vaPu9zHbBNFO9yk5ucc
+85J1eGFrIAqBMiL4jRGUEmUcaAJBABbxV9qsqDIACxCgoWAAFQBFkQlUscrnFBxoFp0KsifFYFKwI+AtS8VhhtcIkc9Fw5olT3dLQYEtZkQlhpERmWQD/ZKAIi88yKjOYrDgWOJSyqyrdSxNl0nOPtlnfFKvlKRGtgElWnS6J6xmokV1AuFusRt4Y6eVOoMbe2DUaYPpXi0fk0ACXnWd2qc3Mpz699n06K3Dcich0gV3JkeuLMTqS5nTlCKjRMDiSaohPWUNiPyO46YoEgyIRBySBgRHhfrAFwb+wom9cGmOht/cu1h2s7FKoRbVYAOu2fOAiNsFXzeAUjkGoxhywXgY7QECkAcQNUBeiKGWY3dhD5T1DAlz3Hq1yF7njQGNUNsN4AZ3frTD3P8bXkDoFvvPxJ2WnjZIA9g+ic81B2yvTJJBf62uhL2CZoY7vO5cUfvPhe4UQqbJyMossy+Dgr3pnVaqSnh7pbGOmJU3Ge3bqSKzmWC1VyZcfJVIgHVAEFAj8gw0+y+Ih+4836ds23Fqse5w/oY4HQFQtIhtLceGCcTplQ560qiGXTOz/Sbs1ie2j6/9XbBCpOjM6YUOe1+RgKIB8UBgXuAueAXiAGeCAPrHi4EwgfG1y/AfFo8HsZIpwsAAAAAElFTkSuQmCC" alt="Twitter" data-bm-cid="<ACE3920D-2E72-45F5-91EB-E0146EB9F7AE@bluemind.net>"></a></p></td></tr></tbody></table>`;
        const result = await InlineImageHelper.insertCid(html, []);

        const expectedHtml = `<table><tbody><tr><td><p style="color: #212121; font-family: Helvetica, sans-serif; font-size: 0.9em;"><a title="Twitter" href="https://twitter.com/_bluemind?lang=fr"><img src="cid:ACE3920D-2E72-45F5-91EB-E0146EB9F7AE@bluemind.net" alt="Twitter" data-bm-cid="<ACE3920D-2E72-45F5-91EB-E0146EB9F7AE@bluemind.net>"></a></p></td></tr></tbody></table>`;
        expect(result.htmlWithCids).toEqual(expectedHtml);
        expect(result.alreadySaved).toEqual([]);
        expect(result.newParts).toEqual([
            {
                address: null,
                mime: "image/png",
                dispositionType: "INLINE",
                encoding: "base64",
                contentId: "<ACE3920D-2E72-45F5-91EB-E0146EB9F7AE@bluemind.net>",
                size: 1923
            }
        ]);
        expect(result.newContentByCid).toEqual({
            "<ACE3920D-2E72-45F5-91EB-E0146EB9F7AE@bluemind.net>": expect.anything()
        });
    });
});
