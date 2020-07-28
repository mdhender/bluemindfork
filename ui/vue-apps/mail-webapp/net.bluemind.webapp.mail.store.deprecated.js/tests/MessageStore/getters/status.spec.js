import { status } from "../../../src/MessageStore/getters/status";

const state = { status: "FamousStatus" };

describe("[Mail-WebappStore/MessageStore][getters] : status ", () => {
    test("Basic", () => {
        const result = status(state);
        expect(result).toEqual("FamousStatus");
    });
});
