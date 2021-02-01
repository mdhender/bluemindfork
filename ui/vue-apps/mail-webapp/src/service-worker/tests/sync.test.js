import FDBFactory from "fake-indexeddb/lib/FDBFactory";
import fetchMock from "fetch-mock";
import { maildb } from "../MailDB";

import { syncMailFolders, syncMyMailbox, syncMailFolder, syncMailbox } from "../sync";

describe("periodicSync", () => {
    beforeEach(() => {
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
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_all", [
            {
                uid: 1,
                internalId: 1,
                name: "folder1"
            },
            {
                uid: 2,
                internalId: 2,
                name: "folder2"
            },
            {
                uid: 3,
                internalId: 3,
                name: "folder3"
            },
            {
                uid: 4,
                internalId: 4,
                name: "folder4"
            }
        ]);
        const dbPromise = maildb.getInstance("user.baz@foo_bar");
        expect(await (await (await dbPromise).dbPromise).getAll("sync_options")).toMatchInlineSnapshot(`Array []`);
        expect(await (await (await dbPromise).dbPromise).getAll("mail_folders")).toMatchInlineSnapshot(`Array []`);
        await syncMyMailbox();
        expect(await (await (await dbPromise).dbPromise).getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
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
                "name": "folder1",
                "uid": 1,
              },
              Object {
                "internalId": 2,
                "name": "folder2",
                "uid": 2,
              },
            ]
        `);
    });

    test("syncMyMailbox for the second time", async () => {
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1", {
            created: [4],
            updated: [2],
            deleted: [1],
            version: 2
        });
        await syncMyMailbox();
        await syncMyMailbox();
        const dbPromise = maildb.getInstance("user.baz@foo_bar");
        expect(await (await (await dbPromise).dbPromise).getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
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
                "name": "folder2",
                "uid": 2,
              },
              Object {
                "internalId": 4,
                "name": "folder4",
                "uid": 4,
              },
            ]
        `);
    });

    test("syncMailFolders", async () => {
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=0", {
            created: [{ id: 1, version: 0 }],
            updated: [],
            deleted: [{ id: 3, version: 0 }],
            version: 1
        });
        fetchMock.mock("/api/mail_items/2/_filteredChangesetById?since=0", {
            created: [{ id: 1, version: 0 }],
            updated: [],
            deleted: [{ id: 1, version: 0 }],
            version: 1
        });
        fetchMock.mock("api/mail_items/1/_multipleById", [
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

        fetchMock.mock("api/mail_items/2/_multipleById", [
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

        const dbPromise = maildb.getInstance("user.baz@foo_bar");

        await syncMailFolders();
        expect(await (await (await dbPromise).dbPromise).getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "type": "mail_item",
                "uid": 1,
                "version": 1,
              },
              Object {
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
    });

    test("consecutive syncMailFolder calls should reuse and update sync_options", async () => {
        fetchMock.reset();
        fetchMock.mock("api/mail_items/1/_multipleById", []);
        const db = await (await maildb.getInstance("user.baz@foo_bar")).dbPromise;
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=0", {
            created: [], updated: [], deleted: [], version: 1
        });

        await syncMailFolder(1);
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=0")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "type": "mail_item",
                "uid": 1,
                "version": 1,
              },
            ]
        `);

        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=1", {
            created: [], updated: [], deleted: [], version: 2
        });
        await syncMailFolder(1);
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=1")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "type": "mail_item",
                "uid": 1,
                "version": 2,
              },
            ]
        `);
    });

    test("syncMailFolder call with a version <= to the one sync should do nothing", async () => {
        fetchMock.reset();
        fetchMock.mock("api/mail_items/1/_multipleById", []);
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=0", {
            created: [], updated: [], deleted: [], version: 1
        });
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=1", {
            created: [], updated: [], deleted: [], version: 2
        });

        await syncMailFolder(1);
        await syncMailFolder(1, 1);
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=0")).toBeTruthy();
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=1")).toBeFalsy();
    });

    test("consecutive syncMailFolder calls should wait for the previous one", async () => {
        const db = await (await maildb.getInstance("user.baz@foo_bar")).dbPromise;
        fetchMock.reset();
        fetchMock.mock("/session-infos", { userId: "baz", domain: "foo.bar" });
        fetchMock.mock(
            "/api/mail_items/1/_filteredChangesetById?since=0",
            { created: [], updated: [], deleted: [], version: 1 },
            { delay: 100 }
        );
        fetchMock.mock("/api/mail_items/1/_filteredChangesetById?since=1", {
            created: [], updated: [], deleted: [], version: 2
        });
        syncMailFolder(1);
        await syncMailFolder(1);
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=0")).toBeTruthy();
        expect(fetchMock.called("/api/mail_items/1/_filteredChangesetById?since=1")).toBeTruthy();
        expect(await db.getAll("sync_options")).toMatchInlineSnapshot(`
            Array [
              Object {
                "type": "mail_item",
                "uid": 1,
                "version": 2,
              },
            ]
        `);
    })

    test("consecutive syncMailbox calls should reuse and update sync_options", async () => {
        fetchMock.reset();
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_all", []);
        const db = await (await maildb.getInstance("user.baz@foo_bar")).dbPromise;
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0", {
            created: [], updated: [], deleted: [], version: 1
        });
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
            created: [], updated: [], deleted: [], version: 2
        });
        await syncMailbox("foo.bar", "baz");
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

    test("syncMailbox call with a version <= to the one sync should do nothing", async () => {
        fetchMock.reset();
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_all", []);
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0", {
            created: [], updated: [], deleted: [], version: 1
        });
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1", {
            created: [], updated: [], deleted: [], version: 2
        });

        await syncMailbox("foo.bar", "baz");
        await syncMailbox("foo.bar", "baz", 1);
        expect(fetchMock.called("/api/mail_folders/foo_bar/user.baz/_changesetById?since=0")).toBeTruthy();
        expect(fetchMock.called("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1")).toBeFalsy();
    });

    test("consecutive syncMailbox calls should wait for the previous one", async () => {
        const db = await (await maildb.getInstance("user.baz@foo_bar")).dbPromise;
        fetchMock.reset();
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_all", []);
        fetchMock.mock(
            "/api/mail_folders/foo_bar/user.baz/_changesetById?since=0",
            { created: [], updated: [], deleted: [], version: 1 },
            { delay: 100 }
        );
        fetchMock.mock("/api/mail_folders/foo_bar/user.baz/_changesetById?since=1", {
            created: [], updated: [], deleted: [], version: 2
        });
        syncMailbox("foo.bar", "baz");
        await syncMailbox("foo.bar", "baz");
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
