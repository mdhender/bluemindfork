import EmailExtractor from "../src/EmailExtractor";

describe("EmailExtractor", () => {
    const testData = [
        { email: "my@mail.com   ", expectedAddress: "my@mail.com", expectedDN: undefined },
        { email: "    My Mail <my@mail.com>", expectedAddress: "my@mail.com", expectedDN: "My Mail" },
        { email: "My Mail     my@mail.com", expectedAddress: "my@mail.com", expectedDN: "My Mail" },
        { email: "My Mail", expectedAddress: null, expectedDN: "My Mail" },
        { email: "", expectedAddress: null, expectedDN: "" },
        { email: undefined, expectedAddress: null, expectedDN: undefined }
    ];
    test.each(testData)("Extract address '%j'", ({ email, expectedAddress }) => {
        expect(EmailExtractor.extractEmail(email)).toBe(expectedAddress);
    });

    test.each(testData)("Extract displayed name '%j'", ({ email, expectedDN }) => {
        expect(EmailExtractor.extractDN(email)).toBe(expectedDN);
    });
});
