import { state } from "../../src/MailboxFoldersStore/state";

describe("[MailItemsStore][state]", () => {
    test("initial state", () => {
        expect(state).toMatchSnapshot();
    });
});
