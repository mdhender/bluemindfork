import { state, mutations } from "../folderList";

describe("folderList store", () => {
    describe("mutations", () => {
        test("TOGGLE_EDIT_FOLDER: define the folder to be edited", () => {
            mutations.TOGGLE_EDIT_FOLDER(state, "1");
            expect(state.folderList.editing).toEqual("1");

            mutations.TOGGLE_EDIT_FOLDER(state, "1");
            expect(state.folderList.editing).toEqual(undefined);

            mutations.TOGGLE_EDIT_FOLDER(state, "1");
            mutations.TOGGLE_EDIT_FOLDER(state, "2");
            expect(state.folderList.editing).toEqual("2");
        });
    });
});
