import FDBFactory from "fake-indexeddb/lib/FDBFactory";
import { MailDBImpl } from "../MailDB";

class MockDate extends Date {
    constructor() {
        super("2001-01-01T01:01:01.001Z");
    }
}

describe("MailDBImpl", () => {
    describe("DB creation", () => {
        beforeEach(() => {
            global.indexedDB = new FDBFactory();
            global.Date = MockDate;
        });

        test("Test Singleton creation", async () => {
            const db = new MailDBImpl("foo");
            expect(db).toMatchInlineSnapshot(`
                MailDBImpl {
                  "dbPromise": Promise {},
                }
            `);
        });
        test("Change name return new db", async () => {
            const db = new MailDBImpl("foo");
            const db2 = new MailDBImpl("bar");
            expect((await db.dbPromise).name).not.toEqual((await db2.dbPromise).name);
        });
    });

    describe("API and store", () => {
        describe("sync_options", () => {
            let db;
            beforeEach(() => {
                global.indexedDB = new FDBFactory();
                db = new MailDBImpl("foo");
            });

            test("getSyncOptions(uid) returns an item with correct uid", async () => {
                const sync_options = [{ uid: "bar" }];

                for (const item of sync_options) {
                    await (await db.dbPromise).add("sync_options", item);
                }
                const actual = await db.getSyncOptions("bar");
                expect(actual).toEqual(sync_options.find(item => item.uid === "bar"));
            });

            test("updateSyncOptions: unkown id create new object", async () => {
                const sync_options = [{ uid: "bar" }];
                for (const item of sync_options) {
                    await (await db.dbPromise).add("sync_options", item);
                }
                const key = await db.updateSyncOptions({ uid: "unknown" });
                expect(key).not.toBeUndefined();
                const actual = await db.getSyncOptions(key);
                expect(actual).toEqual({ uid: "unknown" });
            });

            test("updateSyncOptions: known id update object", async () => {
                const sync_options = [{ uid: "bar", version: 0 }];
                for (const item of sync_options) {
                    await (await db.dbPromise).add("sync_options", item);
                }
                expect(await db.getSyncOptions("bar")).toEqual({ uid: "bar", version: 0 });
                const key = await db.updateSyncOptions({ uid: "bar", version: 1 });
                expect(key).not.toBeUndefined();
                const actual = await db.getSyncOptions(key);
                expect(actual).toEqual({ uid: "bar", version: 1 });
            });

            test("isSubscribed", async () => {
                const sync_options = [{ uid: "bar" }];
                for (const item of sync_options) {
                    await (await db.dbPromise).add("sync_options", item);
                }
                expect(await db.isSubscribed("bar")).toEqual(true);
                expect(await db.isSubscribed("baz")).toEqual(false);
            });
        });

        describe("mail_folders", () => {
            let db;

            beforeEach(() => {
                global.indexedDB = new FDBFactory();
                db = new MailDBImpl("foo");
            });

            test("getAllMailFolders", async () => {
                const folders = [
                    { uid: "foo", mailboxRoot: "1" },
                    { uid: "bar", mailboxRoot: "1" },
                    { uid: "baz", mailboxRoot: "1" },
                    { uid: "biz", mailboxRoot: "2" }
                ];
                for (const item of folders) {
                    await (await db.dbPromise).add("mail_folders", item);
                }
                const actual = await db.getAllMailFolders("1");
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "mailboxRoot": "1",
                        "uid": "bar",
                      },
                      Object {
                        "mailboxRoot": "1",
                        "uid": "baz",
                      },
                      Object {
                        "mailboxRoot": "1",
                        "uid": "foo",
                      },
                    ]
                `);
            });

            test("putMailFolders", async () => {
                const folders = [{ uid: "foo" }, { uid: "bar" }, { uid: "baz" }];
                await db.putMailFolders("1", folders);
                const actual = await (await db.dbPromise).getAll("mail_folders");
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "mailboxRoot": "1",
                        "uid": "bar",
                      },
                      Object {
                        "mailboxRoot": "1",
                        "uid": "baz",
                      },
                      Object {
                        "mailboxRoot": "1",
                        "uid": "foo",
                      },
                    ]
                `);
            });

            test("deleteMailFolders", async () => {
                const folders = [
                    { uid: "foo", internalId: 1, mailboxRoot: "1" },
                    { uid: "bar", internalId: 2, mailboxRoot: "1" },
                    { uid: "baz", internalId: 3, mailboxRoot: "1" },
                    { uid: "biz", internalId: 1, mailboxRoot: "2" }
                ];
                for (const item of folders) {
                    await (await db.dbPromise).add("mail_folders", item);
                }
                await db.deleteMailFolders("1", [1]);
                const actual = await (await db.dbPromise).getAll("mail_folders");
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "internalId": 2,
                        "mailboxRoot": "1",
                        "uid": "bar",
                      },
                      Object {
                        "internalId": 3,
                        "mailboxRoot": "1",
                        "uid": "baz",
                      },
                      Object {
                        "internalId": 1,
                        "mailboxRoot": "2",
                        "uid": "biz",
                      },
                    ]
                `);
            });
        });

        describe("mail_items", () => {
            let db;

            beforeEach(() => {
                global.indexedDB = new FDBFactory();
                db = new MailDBImpl("foo");
            });

            test("putMailItems", async () => {
                const mails = [{ folderUid: "bar", internalId: "baz" }];
                await db.putMailItems(mails);
                const actual = await (await db.dbPromise).getAll("mail_items");
                expect(actual).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "folderUid": "bar",
                        "internalId": "baz",
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
                    await (await db.dbPromise).put("mail_items", item);
                }
                const actual = await db.getMailItems("bar", ["foo"]);
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
            let db;

            beforeEach(() => {
                global.indexedDB = new FDBFactory();
                db = new MailDBImpl("foo");
            });

            test("setMailItemLight", async () => {
                await db.setMailItemLight(
                    "foo",
                    [
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
                    ["baz", "foobar"]
                );
                const actual = await (await db.dbPromise).getAll("mail_item_light");
                expect(actual).toMatchSnapshot();
            });

            test("getAllMailItemLight", async () => {
                await (
                    await db.dbPromise
                ).put(
                    "mail_item_light",
                    {
                        internalId: 123,
                        flags: [],
                        date: 123467890,
                        subject: "s1",
                        size: 1,
                        sender: "to1@to.com"
                    },
                    "foo"
                );
                await (
                    await db.dbPromise
                ).put(
                    "mail_item_light",
                    {
                        internalId: 456,
                        flags: [],
                        date: 9876543210,
                        subject: "s2",
                        size: 2,
                        sender: "to2@to.com"
                    },
                    "bar"
                );
                const actual = await db.getAllMailItemLight("bar");
                expect(actual).toMatchSnapshot();
            });
        });

        describe("reconciliate", () => {
            let db;

            beforeEach(() => {
                global.indexedDB = new FDBFactory();
                db = new MailDBImpl("foo");
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
                await db.reconciliate(data, syncOptions);
                expect(await (await db.dbPromise).getAll("mail_items")).toMatchInlineSnapshot(`
                    Array [
                      Object {
                        "flags": Array [
                          "foobar",
                        ],
                        "folderUid": "foo",
                        "internalId": "bar",
                        "value": Object {
                          "body": Object {
                            "date": 2001-01-01T01:01:01.001Z,
                          },
                        },
                      },
                    ]
                `);
                expect(await (await db.dbPromise).getAll("sync_options")).toEqual([syncOptions]);
                expect(await (await db.dbPromise).getAll("mail_item_light")).toMatchInlineSnapshot(`
                    Array [
                      Array [
                        Object {
                          "date": 2001-01-01T01:01:01.001Z,
                          "flags": Array [
                            "foobar",
                          ],
                          "internalId": "bar",
                          "sender": undefined,
                          "size": undefined,
                          "subject": undefined,
                        },
                        Object {
                          "date": 2001-01-01T01:01:01.001Z,
                          "flags": Array [
                            "foobaz",
                          ],
                          "internalId": "baz",
                          "sender": undefined,
                          "size": undefined,
                          "subject": undefined,
                        },
                        Object {
                          "date": 2001-01-01T01:01:01.001Z,
                          "flags": Array [
                            "foobar",
                          ],
                          "internalId": "foobar",
                          "sender": undefined,
                          "size": undefined,
                          "subject": undefined,
                        },
                      ],
                    ]
                `);
            });
        });
    });
});
