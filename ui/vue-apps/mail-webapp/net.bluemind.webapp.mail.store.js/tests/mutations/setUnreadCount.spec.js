import { setUnreadCount } from "../../src/mutations/setUnreadCount";

describe("[Mail-WebappStore][mutations] : setUnreadCount ", () => {
    const state = {};
    beforeEach(() => {
        state.foldersData = { key1: { unread: 0 }, key2: { unread: 10 }, key3: { something: "value" } };
    });
    test(" set folder unread count", () => {
        setUnreadCount(state, { folderUid: "key1", count: 10 });
        expect(state.foldersData.key1.unread).toEqual(10);
    });
    test(" set count only if >= 0 ", () => {
        setUnreadCount(state, { folderUid: "key2", count: -1 });
        expect(state.foldersData.key2.unread).toEqual(10);
        setUnreadCount(state, { folderUid: "key2", count: 0 });
        expect(state.foldersData.key2.unread).toEqual(0);
    });
    test(" create entry in foldersData if do not exist", () => {
        setUnreadCount(state, { folderUid: "key3", count: 5 });
        expect(state.foldersData.key3.unread).toBeDefined();
        setUnreadCount(state, { folderUid: "key4", count: 5 });
        expect(state.foldersData.key4.unread).toBeDefined();
    });
    test(" only mutate the 'unread' key", () => {
        setUnreadCount(state, { folderUid: "key3", count: 5 });
        expect(state.foldersData.key3.something).toBe("value");
        setUnreadCount(state, { folderUid: "key3", count: 2 });
        expect(state.foldersData.key3.something).toBe("value");
    });
    test(" only mutate foldersData key", () => {
        setUnreadCount(state, { folderUid: "key1", count: 5 });
        expect(Object.keys(state)).toEqual(["foldersData"]);
    });
});
