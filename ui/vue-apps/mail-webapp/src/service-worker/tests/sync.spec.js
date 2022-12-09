import FDBFactory from "fake-indexeddb/lib/FDBFactory";
import fetchMock from "fetch-mock";
import Session from "../session";

import { syncMailFolders, syncMyMailbox, syncMailFolder, syncMailbox } from "../sync";

describe("sync", () => {
    beforeEach(() => {
        Session.clear();
        global.indexedDB = new FDBFactory();
    });
    afterAll(() => {
        fetchMock.reset();
    });

    test("syncMyMailbox", async () => {
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0", {
            created: [1, 2],
            updated: [],
            deleted: [3],
            version: 1
        });
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_mgetById", [
            {
                uid: 1,
                internalId: 1,
                name: "folder1"
            },
            {
                uid: 2,
                internalId: 2,
                name: "folder2"
            }
        ]);
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=0", {
            created: [],
            updated: [],
            deleted: [],
            version: 1
        });
        fetchMock.mock("/api/mail_items/2/_filteredChangesetById?since=0", {
            created: [],
            updated: [],
            deleted: [],
            version: 1
        });
        fetchMock.mock("/api/mail_items/4/_filteredChangesetById?since=0", {
            created: [],
            updated: [],
            deleted: [],
            version: 1
        });
        const dbPromise = Session.db();
        expect(await (await (await dbPromise).dbPromise).getAll("sync_options")).toMatchInlineSnapshot(`Array []`);
        expect(await (await (await dbPromise).dbPromise).getAll("mail_folders")).toMatchInlineSnapshot(`Array []`);
        const updated = await syncMyMailbox();
        expect(updated).toBeTruthy();
        expect(await (await (await dbPromise).dbPromise).getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": 1,
                "version": 1,
              },
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": 2,
                "version": 1,
              },
              Object {
                "type": "mail_folder",
                "uid": "user.baz@foo_bar",
                "version": 1,
              },
            ]
        `);
        expect(await (await (await dbPromise).dbPromise).getAll("mail_folders")).toMatchInlineSnapshot(`
            Array [
              Object {
                "internalId": 1,
                "mailboxRoot": "user.baz",
                "name": "folder1",
                "uid": 1,
              },
              Object {
                "internalId": 2,
                "mailboxRoot": "user.baz",
                "name": "folder2",
                "uid": 2,
              },
            ]
        `);
    });

    test("syncMyMailbox for the second time", async () => {
        let updated = await syncMyMailbox();
        expect(updated).toBeTruthy();
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1", {
            created: [4],
            updated: [2],
            deleted: [1],
            version: 2
        });
        fetchMock.mock("/api/mail_items/2/_filteredChangesetById?since=1", {
            created: [],
            updated: [],
            deleted: [],
            version: 1
        });
        fetchMock.mock(
            "/api/mail_folders/foo_bar/user.baz/_mgetById",
            [
                {
                    uid: 4,
                    internalId: 4,
                    name: "folder4"
                },
                {
                    uid: 2,
                    internalId: 2,
                    name: "folder2"
                }
            ],
            { overwriteRoutes: true }
        );
        updated = await syncMyMailbox();
        expect(updated).toBeTruthy();
        const dbPromise = Session.db();
        expect(await (await (await dbPromise).dbPromise).getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": 1,
                "version": 1,
              },
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": 2,
                "version": 1,
              },
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": 4,
                "version": 1,
              },
              Object {
                "type": "mail_folder",
                "uid": "user.baz@foo_bar",
                "version": 2,
              },
            ]
        `);
        expect(await (await (await dbPromise).dbPromise).getAll("mail_folders")).toMatchInlineSnapshot(`
            Array [
              Object {
                "internalId": 2,
                "mailboxRoot": "user.baz",
                "name": "folder2",
                "uid": 2,
              },
              Object {
                "internalId": 4,
                "mailboxRoot": "user.baz",
                "name": "folder4",
                "uid": 4,
              },
            ]
        `);
    });

    test("syncMailFolders", async () => {
        fetchMock.mock("/api/containers/_subscriptions/foo.bar/baz/_changesetById?since=0", {
            created: ["subscription1"],
            updated: [],
            deleted: [],
            version: 1
        });
        fetchMock.mock("/api/containers/_subscriptions/foo.bar/baz/_mgetById", [
            {
                uid: "subscription1",
                value: {
                    owner: (await Session.infos()).userId,
                    offlineSync: true,
                    containerType: "mailboxacl"
                }
            }
        ]);
        fetchMock.mock(
            "/api/mail_folders/foo_bar/user.baz/_mgetById",
            [
                {
                    uid: 1,
                    internalId: 1,
                    name: "folder1"
                },
                {
                    uid: 2,
                    internalId: 2,
                    name: "folder2"
                }
            ],
            { overwriteRoutes: true }
        );
        fetchMock.mock(
            "/api/mail_items/1/_filteredChangesetById?since=0",
            {
                created: [{ id: 1, version: 0 }],
                updated: [],
                deleted: [{ id: 3, version: 0 }],
                version: 1
            },
            { overwriteRoutes: true }
        );
        fetchMock.mock(
            "/api/mail_items/2/_filteredChangesetById?since=0",
            {
                created: [{ id: 1, version: 0 }],
                updated: [],
                deleted: [{ id: 1, version: 0 }],
                version: 1
            },
            { overwriteRoutes: true }
        );
        fetchMock.mock("api/mail_items/1/_mgetById", [
            {
                internalId: "foobar1",
                folderUid: 1,
                value: {
                    body: {
                        date: new Date("December 17, 1995 03:24:00")
                    }
                }
            },
            {
                internalId: "foobar2",
                folderUid: 1,
                value: {
                    body: {
                        date: new Date("December 17, 1995 03:24:00")
                    }
                }
            }
        ]);

        fetchMock.mock("api/mail_items/2/_mgetById", [
            {
                internalId: "foobar3",
                folderUid: 2,
                value: {
                    body: {
                        date: new Date("December 17, 1995 03:24:00")
                    }
                }
            }
        ]);

        const dbPromise = Session.db();

        await syncMailFolders();

        expect(await (await (await dbPromise).dbPromise).getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": 1,
                "version": 1,
              },
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": 2,
                "version": 1,
              },
              Object {
                "type": "owner_subscriptions",
                "uid": "baz@foo.bar.subscriptions",
                "version": 1,
              },
              Object {
                "type": "mail_folder",
                "uid": "user.baz@foo_bar",
                "version": 1,
              },
            ]
        `);
    });

    test("consecutive syncMailFolder calls should reuse and update sync_options", async () => {
        fetchMock.reset();
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock("api/mail_items/1/_mgetById", []);
        const db = await (await Session.db()).dbPromise;
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=0", {
            created: [],
            updated: [],
            deleted: [],
            version: 1
        });

        let updated = await syncMailFolder(1);
        expect(updated).toBeTruthy();
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=0")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": 1,
                "version": 1,
              },
            ]
        `);

        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=1", {
            created: [],
            updated: [],
            deleted: [],
            version: 2
        });
        updated = await syncMailFolder(1);
        expect(updated).toBeTruthy();
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=1")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": 1,
                "version": 2,
              },
            ]
        `);
    });

    test("syncMailFolder call with a version <= to the one sync should do nothing", async () => {
        fetchMock.reset();
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock("api/mail_items/1/_mgetById", []);
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=0", {
            created: [],
            updated: [],
            deleted: [],
            version: 1
        });
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=1", {
            created: [],
            updated: [],
            deleted: [],
            version: 2
        });

        let updated = await syncMailFolder(1);
        expect(updated).toBeTruthy();
        updated = await syncMailFolder(1, 1);
        expect(updated).toBeFalsy();
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=0")).toBeTruthy();
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=1")).toBeFalsy();
    });

    test("consecutive syncMailFolder calls should wait for the previous one", async () => {
        const db = await (await Session.db()).dbPromise;
        fetchMock.reset();
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock(
            "/api/mail_items/1/_filteredChangesetById?since=0",
            { created: [], updated: [], deleted: [], version: 1 },
            { delay: 100 }
        );
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=1", {
            created: [],
            updated: [],
            deleted: [],
            version: 2
        });
        syncMailFolder(1);
        let updated = await syncMailFolder(1);
        expect(updated).toBeTruthy();
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=0")).toBeTruthy();
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=1")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": 1,
                "version": 2,
              },
            ]
        `);
    });

    test("consecutive syncMailFolder calls without a newer version should return false", async () => {
        fetchMock.reset();
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock("api/mail_items/1/_mgetById", []);
        const db = await (await Session.db()).dbPromise;
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=0", {
            created: [],
            updated: [],
            deleted: [],
            version: 1
        });

        let updated = await syncMailFolder("1");
        expect(updated).toBeTruthy();
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=0")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": "1",
                "version": 1,
              },
            ]
        `);

        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=1", {
            created: [],
            updated: [],
            deleted: [],
            version: 1
        });
        updated = await syncMailFolder("1");
        expect(updated).toBeFalsy();
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=1")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "pending": false,
                "type": "mail_item",
                "uid": "1",
                "version": 1,
              },
            ]
        `);
    });

    test("consecutive syncMailbox calls should reuse and update sync_options", async () => {
        fetchMock.reset();
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_all", []);
        const db = await (await Session.db()).dbPromise;
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0", {
            created: [],
            updated: [],
            deleted: [],
            version: 1
        });
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_mgetById", []);

        await syncMailbox("foo.bar", "baz");
        expect(fetchMock.called("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "type": "mail_folder",
                "uid": "user.baz@foo_bar",
                "version": 1,
              },
            ]
        `);

        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1", {
            created: [],
            updated: [],
            deleted: [],
            version: 2
        });
        const updated = await syncMailbox("foo.bar", "baz");
        expect(updated).toBeTruthy();
        expect(fetchMock.called("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "type": "mail_folder",
                "uid": "user.baz@foo_bar",
                "version": 2,
              },
            ]
        `);
    });

    test("consecutive syncMailbox calls without a newer version should return false", async () => {
        fetchMock.reset();
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_mgetById", [
            {
                uid: 1,
                internalId: 1,
                name: "folder1"
            },
            {
                uid: 2,
                internalId: 2,
                name: "folder2"
            }
        ]);
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0", {
            created: [1],
            updated: [2],
            deleted: [],
            version: 1
        });
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1", {
            created: [],
            updated: [],
            deleted: [],
            version: 1
        });

        const db = await (await Session.db()).dbPromise;
        let updated = await syncMailbox("foo.bar", "baz");
        expect(updated.length).toBeGreaterThan(0);
        expect(fetchMock.called("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "type": "mail_folder",
                "uid": "user.baz@foo_bar",
                "version": 1,
              },
            ]
        `);

        updated = await syncMailbox("foo.bar", "baz");
        expect(updated.length).toBe(0);
        expect(fetchMock.called("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "type": "mail_folder",
                "uid": "user.baz@foo_bar",
                "version": 1,
              },
            ]
        `);
    });

    test("syncMailbox call with a version <= to the one sync should do nothing", async () => {
        fetchMock.reset();
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_mgetById", [
            {
                uid: 1,
                internalId: 1,
                name: "folder1"
            },
            {
                uid: 2,
                internalId: 2,
                name: "folder2"
            }
        ]);
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0", {
            created: [1],
            updated: [2],
            deleted: [],
            version: 1
        });
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1", {
            created: [],
            updated: [],
            deleted: [],
            version: 2
        });

        let updated = await syncMailbox("foo.bar", "baz");
        expect(updated.length).toBeGreaterThan(0);
        updated = await syncMailbox("foo.bar", "baz", 1);
        expect(updated.length).toBe(0);
        expect(fetchMock.called("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0")).toBeTruthy();
        expect(fetchMock.called("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1")).toBeFalsy();
    });

    test("consecutive syncMailbox calls should wait for the previous one", async () => {
        const db = await (await Session.db()).dbPromise;
        fetchMock.reset();
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_mgetById", []);
        fetchMock.mock(
            "/api/mail_folders/foo_bar/user.baz/_changesetById?since=0",
            { created: [], updated: [], deleted: [], version: 1 },
            { delay: 100 }
        );
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1", {
            created: [],
            updated: [],
            deleted: [],
            version: 2
        });
        syncMailbox("foo.bar", "baz");
        let updated = await syncMailbox("foo.bar", "baz");
        expect(updated).toBeTruthy();
        expect(fetchMock.called("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0")).toBeTruthy();
        expect(fetchMock.called("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "type": "mail_folder",
                "uid": "user.baz@foo_bar",
                "version": 2,
              },
            ]
        `);
    });
});
