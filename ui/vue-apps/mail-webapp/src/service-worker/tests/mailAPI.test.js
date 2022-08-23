import fetchMock from "fetch-mock";
import Session from "../session";

describe("mailAPI", () => {
    describe("sessionInfos", () => {
        const somesessioninfos = {
            userId: "foo",
            domain: "bar"
        };
        const route = "/session-infos";
        beforeAll(() => {
            Session.clear();
            fetchMock.mock(route, somesessioninfos);
        });
        afterAll(() => {
            fetchMock.reset();
        });
        beforeEach(() => {
            fetchMock.resetHistory();
        });
        test("userAtDomain", async () => {
            const actual = await Session.userAtDomain();
            expect(actual).toEqual(`user.${somesessioninfos.userId}@${somesessioninfos.domain}`);
        });
        test("one call", async () => {
            const actual = await Session.infos();
            expect(actual).toEqual(somesessioninfos);
        });
        test("multiple calls", async () => {
            const firstinstance = await Session.infos();
            expect(firstinstance).toEqual(somesessioninfos);
            expect(fetchMock.calls(route).length).toEqual(0);
            Session.clear();
            const secondInstance = await Session.infos();
            expect(secondInstance).toEqual(somesessioninfos);
            expect(fetchMock.calls(route).length).toEqual(1);
        });

        test("userAtDomain", async () => {
            const actual = await Session.userAtDomain();
            expect(actual).toEqual("user.foo@bar");
        });
    });
    describe("mailapi", () => {
        const sid = "foobar";
        beforeAll(() => {
            Session.clear();
            fetchMock.mock("/session-infos", { sid });
            fetchMock.mock(/\/api\/mail_items\/(.*)\/_mgetById/, {});
            fetchMock.mock(/\/api\/mail_items\/(.*)\/_filteredChangesetById/, {});
            fetchMock.mock(/\/api\/mail_folders\/(.*)\/_mgetById/, {});
            fetchMock.mock(/\/api\/mail_folders\/(.*)\/_changesetById/, {});
        });
        afterAll(() => {
            fetchMock.reset();
        });
        test("get instance", async () => {
            expect(await Session.api()).not.toBeNull();
            expect(fetchMock.lastUrl()).toEqual("/session-infos");
            Session.clear();
            expect(await Session.api()).not.toBeNull();
            expect(fetchMock.lastUrl()).toEqual("/session-infos");
        });
        test("mailItem.mget", async () => {
            const api = await Session.api();
            const body = [1, 2, 3];
            const uid = "foo";
            const actual = await api.mailItem.mget(uid, body);
            expect(actual).toEqual({});
            expect(fetchMock.lastCall()).toMatchInlineSnapshot(`
                Array [
                  "/api/mail_items/foo/_mgetById",
                  Object {
                    "body": "[1,2,3]",
                    "credentials": "include",
                    "headers": Object {
                      "x-bm-apikey": "foobar",
                    },
                    "method": "POST",
                    "mode": "cors",
                  },
                ]
            `);
        });
        test("mailItem.changeset", async () => {
            const api = await Session.api();
            const version = 0;
            const uid = "foo";
            const actual = await api.mailItem.filteredChangeset(uid, version);
            expect(actual).toEqual({});
            expect(fetchMock.lastCall()).toMatchInlineSnapshot(`
                Array [
                  "/api/mail_items/foo/_filteredChangesetById?since=0",
                  Object {
                    "body": "{\\"must\\":[],\\"mustNot\\":[\\"Deleted\\"]}",
                    "credentials": "include",
                    "headers": Object {
                      "x-bm-apikey": "foobar",
                    },
                    "method": "POST",
                    "mode": "cors",
                  },
                ]
            `);
        });
        test("mailFolder.mget", async () => {
            const api = await Session.api();
            const mailboxRoot = "user.bar";
            const domain = "baz";
            const actual = await api.mailFolder.mget({ mailboxRoot, domain }, [1, 2, 3]);
            expect(actual).toEqual({});
            expect(fetchMock.lastCall()).toMatchInlineSnapshot(`
                Array [
                  "/api/mail_folders/baz/user.bar/_mgetById",
                  Object {
                    "body": "[1,2,3]",
                    "credentials": "include",
                    "headers": Object {
                      "x-bm-apikey": "foobar",
                    },
                    "method": "POST",
                    "mode": "cors",
                  },
                ]
            `);
        });
        test("mailFolder.changeset", async () => {
            const api = await Session.api();
            const mailboxRoot = "user.bar";
            const domain = "baz";
            const version = 0;
            const actual = await api.mailFolder.changeset({ mailboxRoot, domain }, version);
            expect(actual).toEqual({});
            expect(fetchMock.lastCall()).toMatchInlineSnapshot(`
                Array [
                  "/api/mail_folders/baz/user.bar/_changesetById?since=0",
                  Object {
                    "credentials": "include",
                    "headers": Object {
                      "x-bm-apikey": "foobar",
                    },
                    "mode": "cors",
                  },
                ]
            `);
        });
    });

    describe("handling error", () => {
        test("Unauthorized", async () => {
            fetchMock.reset();
            fetchMock.mock("/session-infos", 401);
            Session.clear();
            await expect(Session.infos()).rejects.toEqual("401 Unauthorized");
        });
        test("Other error", async () => {
            fetchMock.reset();
            fetchMock.mock("/session-infos", 500);
            Session.clear();
            await expect(Session.infos()).rejects.toEqual("Error in BM API 500");
        });
    });
});
