import mergeContacts from "../src/mergeContacts";

jest.mock("@bluemind/inject", () => {
    const getCompleteBookResults = require("./data/getCompleteBookResults.json");
    return {
        inject: service => {
            if (service === "AddressBooksMgmtPersistence") {
                return { getComplete: uid => getCompleteBookResults[uid] };
            }
            if (service === "UserSession") {
                return { userId: "", domain: "" };
            }
        }
    };
});

global.fetch = jest.fn(() => "photoBinary");

describe("Merge contacts", () => {
    test("Merge contacts from different address books", async () => {
        const searchResults = require("./data/searchResults.json");
        const getCompleteResults = require("./data/getCompleteResults.json");
        const contact = await mergeContacts(
            getCompleteResults,
            searchResults.values.map(v => v.containerUid)
        );
        expect(contact).toMatchSnapshot();
    });
});
