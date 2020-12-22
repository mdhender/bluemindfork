import store from "../folderList";
import { TOGGLE_EDIT_FOLDER, SET_MAILBOX_FOLDERS } from "~mutations";
import { MailboxType } from "../../model/mailbox";

describe("folderList store", () => {
    describe("mutations", () => {
        test("TOGGLE_EDIT_FOLDER: define the folder to be edited", () => {
            store.mutations[TOGGLE_EDIT_FOLDER](store.state, "1");
            expect(store.state.editing).toEqual("1");

            store.mutations[TOGGLE_EDIT_FOLDER](store.state, "1");
            expect(store.state.editing).toEqual(undefined);

            store.mutations[TOGGLE_EDIT_FOLDER](store.state, "1");
            store.mutations[TOGGLE_EDIT_FOLDER](store.state, "2");
            expect(store.state.editing).toEqual("2");
        });
        test("SET_MAILBOX_FOLDERS: Must change mailbox loading state", () => {
            expect(store.state.myMailboxIsLoaded).toBeFalsy();
            store.mutations[SET_MAILBOX_FOLDERS](store.state, { mailbox: { type: MailboxType.USER } });
            expect(store.state.myMailboxIsLoaded).toBeTruthy();
            expect(store.state.mailsharesAreLoaded).toBeFalsy();
            store.mutations[SET_MAILBOX_FOLDERS](store.state, { mailbox: { type: MailboxType.MAILSHARE } });
            expect(store.state.mailsharesAreLoaded).toBeTruthy();
        });
    });
});
