import { state } from "../../src/MailboxItemsStore/state";

describe("[MailItemsStore][state]", () => {
    test("initial state", () => {
        expect(state).toMatchSnapshot();
    });
});
