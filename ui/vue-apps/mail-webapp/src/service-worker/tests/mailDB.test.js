import FDBFactory from "fake-indexeddb/lib/FDBFactory";
import { maildb } from "../MailDB";

describe("MailDB", () => {
    describe("DB creation", () => {
        beforeEach(() => {
            global.indexedDB = new FDBFactory();
        });

        test("Test Singleton creation", async () => {
            const db = await maildb.getInstance("foo");
            expect(db).toMatchInlineSnapshot(`
                MailDB {
                  "dbPromise": Promise {},
                }
            `);
        });
        test("Change name return new db", async () => {
            const db = await maildb.getInstance("foo");
            const db2 = await maildb.getInstance("bar");
            expect((await db.dbPromise).name).not.toEqual((await db2.dbPromise).name);
        });
    });

    describe("API & store", () => {
        describe("sync_options", () => {
            let dbPromise;
            beforeEach(() => {
                global.indexedDB = new FDBFactory();
                dbPromise = maildb.getInstance("foo");
            });

            test("getSyncOptions(uid) returns an item with correct uid", async () => {
                const sync_options = [{ uid: "bar" }];

                for (const item of sync_options) {
                    (await (await dbPromise).dbPromise).add("sync_options", item);
                }
                const actual = await (await dbPromise).getSyncOptions("bar");
                expect(actual).toEqual(sync_options.find(item => item.uid === "bar"));
            });

            test("getAllSyncOptions", async () => {
                const sync_options = [{ uid: "bar" }];
                for (const item of sync_options) {
                    (await (await dbPromise).dbPromise).add("sync_options", item);
                }
                const actual = await (await dbPromise).getAllSyncOptions();
                expect(actual).toEqual(sync_options);
            });

            test("getAllSyncOptions by type", async () => {
                const sync_options = [{ uid: "bar", type: "baz" }];
                for (const item of sync_options) {
                    (await (await dbPromise).dbPromise).add("sync_options", item);
                }
                const actual = await (await dbPromise).getAllSyncOptions("baz");
                expect(actual).toEqual(sync_options.filter(item => item.type === "baz"));
            });

            test("updateSyncOptions: unkown id create new object", async () => {
                const sync_options = [{ uid: "bar" }];
                for (const item of sync_options) {
                    (await (await dbPromise).dbPromise).add("sync_options", item);
                }
                const key = await (await dbPromise).updateSyncOptions({ uid: "unknown" });
                expect(key).not.toBeUndefined();
                const actual = await (await dbPromise).getSyncOptions(key);
                expect(actual).toEqual({ uid: "unknown" });
            });

            test("updateSyncOptions: known id update object", async () => {
                const sync_options = [{ uid: "bar", version: 0 }];
                for (const item of sync_options) {
                    (await (await dbPromise).dbPromise).add("sync_options", item);
                }
                expect(await (await dbPromise).getSyncOptions("bar")).toEqual({ uid: "bar", version: 0 });
                const key = await (await dbPromise).updateSyncOptions({ uid: "bar", version: 1 });
                expect(key).not.toBeUndefined();
                const actual = await (await dbPromise).getSyncOptions(key);
                expect(actual).toEqual({ uid: "bar", version: 1 });
            });

            test("isSubscribed", async () => {
                const sync_options = [{ uid: "bar" }];
                for (const item of sync_options) {
                    (await (await dbPromise).dbPromise).add("sync_options", item);
                }
                expect(await (await dbPromise).isSubscribed("bar")).toEqual(true);
                expect(await (await dbPromise).isSubscribed("baz")).toEqual(false);
            });
        });

        describe("mail_folders", () => {
            let dbPromise;

            beforeEach(() => {
                global.indexedDB = new FDBFactory();
                dbPromise = maildb.getInstance("foo");
            });

            test("getAllMailFolders", async () => {
                const folders = [{ uid: "foo" }, { uid: "bar" }, { uid: "baz" }];
                for (const item of folders) {
                    (await (await dbPromise).dbPromise).add("mail_folders", item);
                }
                const actual = await (await dbPromise).getAllMailFolders();
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "uid": "bar",
                      },
                      Object {
                        "uid": "baz",
                      },
                      Object {
                        "uid": "foo",
                      },
                    ]
                `);
            });

            test("putMailFolders", async () => {
                const folders = [{ uid: "foo" }, { uid: "bar" }, { uid: "baz" }];
                await (await dbPromise).putMailFolders(folders);
                const actual = await (await (await dbPromise).dbPromise).getAll("mail_folders");
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "uid": "bar",
                      },
                      Object {
                        "uid": "baz",
                      },
                      Object {
                        "uid": "foo",
                      },
                    ]
                `);
            });

            test("deleteMailFolders", async () => {
                const folders = [
                    { uid: "foo", internalId: 1 },
                    { uid: "bar", internalId: 2 },
                    { uid: "baz", internalId: 3 }
                ];
                for (const item of folders) {
                    (await (await dbPromise).dbPromise).add("mail_folders", item);
                }
                await (await dbPromise).deleteMailFolders([1]);
                const actual = await (await (await dbPromise).dbPromise).getAll("mail_folders");
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "internalId": 2,
                        "uid": "bar",
                      },
                      Object {
                        "internalId": 3,
                        "uid": "baz",
                      },
                    ]
                `);
            });
        });

        describe("mail_items", () => {
            let dbPromise;

            beforeEach(() => {
                global.indexedDB = new FDBFactory();
                dbPromise = maildb.getInstance("foo");
            });

            test("putMailItems", async () => {
                const mails = [{ folderUid: "bar", internalId: "baz" }];
                await (await dbPromise).putMailItems(mails);
                const actual = await (await (await dbPromise).dbPromise).getAll("mail_items");
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "folderUid": "bar",
                        "internalId": "baz",
                      },
                    ]
                `);
            });

            test("getAllMailItems", async () => {
                const mails = [
                    { folderUid: "bar", internalId: "baz" },
                    { folderUid: "bar", internalId: "foo" },
                    { folderUid: "baz", internalId: "foo" }
                ];
                for (const item of mails) {
                    (await (await dbPromise).dbPromise).put("mail_items", item);
                }
                const actual = await (await dbPromise).getAllMailItems("bar");
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "folderUid": "bar",
                        "internalId": "baz",
                      },
                      Object {
                        "folderUid": "bar",
                        "internalId": "foo",
                      },
                    ]
                `);
            });

            test("getMailItems", async () => {
                const mails = [
                    { folderUid: "bar", internalId: "baz" },
                    { folderUid: "bar", internalId: "foo" },
                    { folderUid: "baz", internalId: "foo" }
                ];
                for (const item of mails) {
                    (await (await dbPromise).dbPromise).put("mail_items", item);
                }
                const actual = await (await dbPromise).getMailItems("bar", ["foo"]);
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "folderUid": "bar",
                        "internalId": "foo",
                      },
                    ]
                `);
            });
        });

        describe("mail_items_light", () => {
            let dbPromise;

            beforeEach(() => {
                global.indexedDB = new FDBFactory();
                dbPromise = maildb.getInstance("foo");
            });

            test("putMailItemLight", async () => {
                await (await dbPromise).putMailItemLight([
                    {
                        folderUid: "bar",
                        internalId: "foo"
                    }
                ]);
                const actual = await (await (await dbPromise).dbPromise).getAll("mail_item_light");
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "folderUid": "bar",
                        "internalId": "foo",
                      },
                    ]
                `);
            });

            test("getAllMailItemLight", async () => {
                const mails = [
                    { folderUid: "bar", internalId: "baz" },
                    { folderUid: "bar", internalId: "foo" },
                    { folderUid: "baz", internalId: "foo" }
                ];
                for (const item of mails) {
                    (await (await dbPromise).dbPromise).put("mail_item_light", item);
                }
                const actual = await (await dbPromise).getAllMailItemLight("bar");
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "folderUid": "bar",
                        "internalId": "baz",
                      },
                      Object {
                        "folderUid": "bar",
                        "internalId": "foo",
                      },
                    ]
                `);
            });
        });

        describe("reconciliate", () => {
            let dbPromise;

            beforeEach(() => {
                global.indexedDB = new FDBFactory();
                dbPromise = maildb.getInstance("foo");
            });

            test("reconciliate some data", async () => {
                const syncOptions = { uid: "foo" };
                const data = {
                    items: [
                        {
                            internalId: "baz",
                            flags: ["foobaz"],
                            value: {
                                body: {
                                    date: new Date()
                                }
                            }
                        },
                        {
                            internalId: "bar",
                            flags: ["foobar"],
                            value: {
                                body: {
                                    date: new Date("December 17, 1995 03:24:00")
                                }
                            }
                        },
                        {
                            internalId: "foobar",
                            flags: ["foobar"],
                            value: {
                                body: {
                                    date: new Date("December 17, 1995 03:24:00")
                                }
                            }
                        }
                    ],
                    uid: "foo",
                    deletedIds: ["baz", "foobar"]
                };
                (await dbPromise).reconciliate(data, syncOptions);
                expect(await (await (await dbPromise).dbPromise).getAll("mail_items")).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "flags": Array [
                          "foobar",
                        ],
                        "folderUid": "foo",
                        "internalId": "bar",
                        "value": Object {
                          "body": Object {
                            "date": 1995-12-17T03:24:00.000Z,
                          },
                        },
                      },
                    ]
                `);
                expect(await (await (await dbPromise).dbPromise).getAll("sync_options")).toEqual([syncOptions]);
                expect(await (await (await dbPromise).dbPromise).getAll("mail_item_light")).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "date": 1995-12-17T03:24:00.000Z,
                        "flags": Array [
                          "foobar",
                        ],
                        "folderUid": "foo",
                        "internalId": "bar",
                      },
                    ]
                `);
            });
        });
    });
});
