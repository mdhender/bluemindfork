import { fetchContactMembers } from "../src";

jest.mock("@bluemind/inject", () => ({
    inject: () => ({
        getComplete: uid => {
            if (uid === "group-uid") {
                return require("./group.vcard.json");
            }
            if (uid === "request-error-uid") {
                throw "TestError";
            }
            return undefined;
        }
    })
}));

describe("Contact", () => {
    test("fetchContactMembers can handle members without uid or not found on server and does not fail on request error", async () => {
        const res = await fetchContactMembers("address-book-uid", "group-uid");
        expect(res?.length).toBe(6);
        res.forEach(c => {
            expect(c.kind).toBe("individual");
            expect(c.dn).toBeTruthy();
            expect(c.address).toBeTruthy();
        });
    });
});
