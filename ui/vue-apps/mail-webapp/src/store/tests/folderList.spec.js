import store from "../folderList";
import { TOGGLE_EDIT_FOLDER } from "~/mutations";

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
    });
});
