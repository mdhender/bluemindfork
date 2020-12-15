import fetchMock from "fetch-mock";
import { sessionInfos, mailapi, userAtDomain, getDBName } from "../MailAPI";

describe("mailAPI", () => {
    test("userAtDomain", () => {
        const userId = "foo";
        const domain = "bar";
        const actual = userAtDomain({ userId, domain });
        expect(actual).toEqual(`user.${userId}@${domain}`);
    });

    describe("sessionInfos", () => {
        const somesessioninfos = {
            userId: "foo",
            domain: "bar"
        };
        const route = "/session-infos";
        beforeAll(() => {
            sessionInfos.clear();
            fetchMock.mock(route, somesessioninfos);
        });
        afterAll(() => {
            fetchMock.reset();
        });
        beforeEach(() => {
            fetchMock.resetHistory();
        });
        test("one call", async () => {
            const actual = await sessionInfos.getInstance();
            expect(actual).toEqual(somesessioninfos);
        });
        test("multiple calls", async () => {
            const firstinstance = await sessionInfos.getInstance();
            expect(firstinstance).toEqual(somesessioninfos);
            expect(fetchMock.calls(route).length).toEqual(0);
            sessionInfos.clear();
            const secondInstance = await sessionInfos.getInstance();
            expect(secondInstance).toEqual(somesessioninfos);
            expect(fetchMock.calls(route).length).toEqual(1);
        });

        test("getDBName", async () => {
            const actual = await getDBName();
            expect(actual).toEqual("user.foo@bar");
        });
    });
    describe("mailapi", () => {
        const sid = "foobar";
        beforeAll(() => {
            sessionInfos.clear();
            fetchMock.mock("/session-infos", { sid });
            fetchMock.mock(/\/api\/mail_items\/(.*)\/_multipleById/, {});
            fetchMock.mock(/\/api\/mail_items\/(.*)\/_filteredChangesetById/, {});
            fetchMock.mock(/\/api\/mail_folders\/(.*)\/_all/, {});
            fetchMock.mock(/\/api\/mail_folders\/(.*)\/_changesetById/, {});
        });
        afterAll(() => {
            fetchMock.reset();
        });
        test("get instance", async () => {
            expect(await mailapi.getInstance()).not.toBeNull();
            expect(fetchMock.lastUrl()).toEqual("/session-infos");
            mailapi.clear();
            expect(await mailapi.getInstance()).not.toBeNull();
            expect(fetchMock.lastUrl()).toEqual("/session-infos");
        });
        test("mailItem.fetch", async () => {
            const api = await mailapi.getInstance();
            const body = [1, 2, 3];
            const uid = "foo";
            const actual = await api.mailItem.fetch(uid, body);
            expect(actual).toEqual({});
            expect(fetchMock.lastCall()).toMatchInlineSnapshot(`
                Array [
                  "/api/mail_items/foo/_multipleById",
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
            const api = await mailapi.getInstance();
            const version = 0;
            const uid = "foo";
            const actual = await api.mailItem.changeset(uid, version);
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
        test("mailFolder.fetch", async () => {
            const api = await mailapi.getInstance();
            const userId = "bar";
            const domain = "baz";
            const actual = await api.mailFolder.fetch({ userId, domain });
            expect(actual).toEqual({});
            expect(fetchMock.lastCall()).toMatchInlineSnapshot(`
                Array [
                  "/api/mail_folders/baz/user.bar/_all",
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
        test("mailFolder.changeset", async () => {
            const api = await mailapi.getInstance();
            const userId = "bar";
            const domain = "baz";
            const version = 0;
            const actual = await api.mailFolder.changeset({ userId, domain }, version);
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
            sessionInfos.clear();
            await expect(sessionInfos.getInstance()).rejects.toEqual("401 Unauthorized");
        });
        test("Other error", async () => {
            fetchMock.reset();
            fetchMock.mock("/session-infos", 500);
            sessionInfos.clear();
            await expect(sessionInfos.getInstance()).rejects.toEqual("Error in BM API 500");
        });
    });
});
