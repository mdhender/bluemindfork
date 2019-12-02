import { unreadCount } from "../../src/getters/unreadCount";

const state = { foldersData: { key1: { unread: 5 }, key2: {} } };

describe("[Mail-WebappStore][getters] : unreadCount ", () => {
    test("return folder unread count instance if known ", () => {
        const fn = unreadCount(state);
        expect(fn("key1")).toEqual(5);
    });
    test("return zero if folder unread count is unknown ", () => {
        const fn = unreadCount(state);
        expect(fn("key2")).toEqual(0);
        expect(fn("key3")).toEqual(0);
    });
});
