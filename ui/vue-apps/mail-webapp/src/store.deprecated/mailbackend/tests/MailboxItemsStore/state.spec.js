import { state } from "../../MailboxItemsStore/state";

describe("[MailItemsStore][state]", () => {
    test("initial state", () => {
        expect(state).toMatchSnapshot();
    });
});
