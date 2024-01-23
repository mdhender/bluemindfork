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

    test("Extract {address, dn} entries from string", () => {
        const str =
            "toto@bluemind.net; <Tata Blue> tata@bluemind.net  ,titi@bluemind.net   <Plop Mind>plop@bluemind.net, Tutu Behem tutu@bluemind.net   ;   \"Yoyo Net\" yoyo@bluemind.net, 'Ri Fifi BM' ri.fifi@bluemind.net, <Georges A.> gabitbol@devenv.dev.bluemind.net";
        expect(EmailExtractor.extractEmails(str)).toEqual([
            { address: "toto@bluemind.net", dn: "" },
            { address: "tata@bluemind.net", dn: "Tata Blue" },
            { address: "titi@bluemind.net", dn: "" },
            { address: "plop@bluemind.net", dn: "Plop Mind" },
            { address: "tutu@bluemind.net", dn: "Tutu Behem" },
            { address: "yoyo@bluemind.net", dn: "Yoyo Net" },
            { address: "ri.fifi@bluemind.net", dn: "Ri Fifi BM" },
            { address: "gabitbol@devenv.dev.bluemind.net", dn: "Georges A." }
        ]);
    });
});
