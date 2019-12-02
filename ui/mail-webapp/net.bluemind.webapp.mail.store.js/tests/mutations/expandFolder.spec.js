import { expandFolder } from "../../src/mutations/expandFolder";

describe("[Mail-WebappStore][mutations] : expandFolder ", () => {
    const state = {};
    beforeEach(() => {
        state.foldersData = { key: { expanded: false }, key2: { something: "else" } };
    });
    test(" mutate folderData for the given folder ", () => {
        expandFolder(state, "key");
        expect(state.foldersData["key"].expanded).toBeTruthy();
        expandFolder(state, "key2");
        expect(state.foldersData["key2"].expanded).toBeTruthy();
    });
    test(" only mutate folderData of the given folder ", () => {
        expandFolder(state, "key2");
        expect(state.foldersData["key2"].expanded).toBeTruthy();
    });
    test(" only mutate expanded key", () => {
        expandFolder(state, "key2");
        expect(state.foldersData["key2"].something).toEqual("else");
    });
    test(" initiate folderData for the given folder if not present", () => {
        expandFolder(state, "key3");
        expect(state.foldersData["key3"]).toEqual({ expanded: true });
    });
    test(" only mutate folderData ", () => {
        expandFolder(state, "key");
        expect(Object.keys(state)).toEqual(["foldersData"]);
    });
});
