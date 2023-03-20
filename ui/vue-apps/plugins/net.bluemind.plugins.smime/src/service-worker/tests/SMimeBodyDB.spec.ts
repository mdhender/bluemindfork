import fetchMock from "fetch-mock";
import FDBFactory from "fake-indexeddb/lib/FDBFactory";
import sMimeBodyDB from "../smime/cache/SMimeBodyDB";

const userId = "58a1ee1b-0c30-492c-a83f-4396f0a24730";
fetchMock.mock("/session-infos", {
    userId: userId
});

const body = { guid: "123", subject: "aaa" };

describe("SMimeBodyDB", () => {
    beforeEach(() => {
        global.indexedDB = new FDBFactory();
    });
    describe("getBody and setBody", () => {
        test("getBody", async () => {
            await sMimeBodyDB.setBody("123", body);
            const res = await sMimeBodyDB.getBody("123");
            expect(res).toEqual(body);
        });
        test("setBody overwrites value if alerady set", async () => {
            const body2 = { guid: "123", subject: "bbb" };
            await sMimeBodyDB.setBody("123", body);
            await sMimeBodyDB.setBody("123", body2);
            const res = await sMimeBodyDB.getBody("123");
            expect(res).toEqual(body2);
        });
    });

    describe("getGuid and setGuid", () => {
        test("getGuid", async () => {
            await sMimeBodyDB.setGuid("folderUid", 100, "guid");
            const res = await sMimeBodyDB.getGuid("folderUid", 100);
            expect(res).toEqual("guid");
        });
        test("setGuid overwrites value is already set in DB", async () => {
            await sMimeBodyDB.setGuid("folderUid", 100, "guid1");
            await sMimeBodyDB.setGuid("folderUid", 100, "guid2");
            const res = await sMimeBodyDB.getGuid("folderUid", 100);
            expect(res).toEqual("guid2");
        });
    });
    describe("deleteBody", () => {
        test("deleteBody deletes an existing body in DB", async () => {
            await sMimeBodyDB.setBody("123", body);
            await sMimeBodyDB.deleteBody("123");
            const res = await sMimeBodyDB.getBody("123");
            expect(res).toBeUndefined();
        });
    });
    describe("invalidate", () => {
        test("invalidate all guid and body entries created more than 7 days before", async () => {
            const recentGuid = "recent";
            await sMimeBodyDB.setGuid("folderUid", 100, recentGuid);
            await sMimeBodyDB.setBody(recentGuid, body);

            const oldTime = 1679417184000;
            Date.now = jest.fn(() => oldTime);
            const oldGuid = "old";
            const oldBody = { guid: oldGuid, subject: "bbb" };
            await sMimeBodyDB.setGuid("folderUid", 100, oldGuid);
            await sMimeBodyDB.setBody(oldGuid, oldBody);

            const oldGuid2 = "old2";
            const oldBody2 = { guid: oldGuid2, subject: "ccc" };
            await sMimeBodyDB.setGuid("folderUid", 102, oldGuid2);
            await sMimeBodyDB.setBody(oldGuid2, oldBody2);

            await sMimeBodyDB.invalidate(oldTime + 1);

            const old = await sMimeBodyDB.getBody(oldGuid);
            expect(old).toBeUndefined();
            const old2 = await sMimeBodyDB.getBody(oldGuid2);
            expect(old2).toBeUndefined();

            const recent = await sMimeBodyDB.getBody(recentGuid);
            expect(recent).toEqual(body);
        });
    });
});
