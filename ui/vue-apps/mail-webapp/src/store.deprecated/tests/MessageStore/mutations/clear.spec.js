import { clear } from "../../../MessageStore/mutations/clear";

const state = {
    key: "AD1DZSD4"
};

describe("[Mail-WebappStore/MessageStore][mutations] : clear", () => {
    test("Basic", () => {
        clear(state);
        expect(state.key).toBeUndefined();
    });
});
