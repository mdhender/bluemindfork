import fetchMock from "fetch-mock";
import { Request, Response, Headers } from "node-fetch";

global.Response = Response;
global.Headers = Headers;

import { registerRoute } from "workbox-routing";
import registerApiRoute, {
    sortMessageByDate,
    filterByFlags,
    allMailFolders,
    unreadItems,
    multipleById,
    filteredChangesetById
} from "../workbox/registerApiRoute";
import { maildb } from "../MailDB";

jest.mock("workbox-routing");
jest.mock("../MailDB");
jest.mock("../sync");

describe("workbox", () => {
    describe("register API Route", () => {
        describe("utils", () => {
            test("sortMessageByDate", () => {
                const item1 = { date: new Date("December 17, 1995 03:24:00") };
                const item2 = { date: new Date("December 31, 1995 03:24:00") };
                const item3 = { date: new Date("December 1, 1995 03:24:00") };
                const item4 = { date: new Date("December 17, 1995 03:25:00") };
                expect([item3, item2, item4, item1].sort(sortMessageByDate)).toEqual([item2, item4, item1, item3]);
            });
            test("filterByFlags", () => {
                const expectedFlags = {
                    must: ["flag1"],
                    mustNot: ["flag2"]
                };
                expect(filterByFlags(expectedFlags, ["flag1"])).toEqual(true);
                expect(filterByFlags(expectedFlags, ["flag2"])).toEqual(false);
            });
        });
        test("check the registration of routes", () => {
            function identity(...args) {
                return args;
            }
            const routes = [
                { capture: /regexp1/, handler: identity },
                { capture: /regexp2/, handler: identity }
            ];
            registerApiRoute(routes);
            expect(registerRoute).toHaveBeenCalledTimes(4 * routes.length);
            expect(registerRoute).toHaveBeenLastCalledWith(/regexp2/, identity, "DELETE");
        });

        describe("route handling functions", () => {
            beforeAll(() => {
                fetchMock.mock("/session-infos", { userId: "foo", domain: "foo.bar" });
                fetchMock.mock("/fakeapi", ["foo"]);
            });

            const handlers = [
                [unreadItems, ["folderUid"], "getAllMailItems", new Request("/fakeapi")],
                [allMailFolders, ["domain", "userId"], "getAllMailFolders", new Request("/fakeapi")],
                [
                    multipleById,
                    ["folderUid"],
                    "getMailItems",
                    new Request("/fakeapi", { method: "POST", body: JSON.stringify([1, 2, 3]) })
                ],
                [
                    filteredChangesetById,
                    ["folderUid"],
                    "getAllMailItemLight",
                    new Request("/fakeapi", {
                        method: "POST",
                        body: JSON.stringify({ must: [], mustNot: ["Deleted", "Seen"] })
                    })
                ]
            ];

            describe.each(handlers)(`%p handler`, (fn, params, api, request) => {
                test("network if not subscribed", async () => {
                    maildb.getInstance.mockReturnValue({
                        isSubscribed: () => false
                    });
                    const actual = await fn({
                        request,
                        params
                    });
                    expect(actual.status).toEqual(200);
                    expect(await actual.json()).toEqual(["foo"]);
                });

                test("local DB if subscribed", async () => {
                    maildb.getInstance.mockReturnValue({
                        isSubscribed: jest.fn().mockResolvedValue(true),
                        [api]: jest.fn().mockResolvedValue([{ internalId: "12345", name: "bar", flags: ["foobar"] }])
                    });
                    const actual = await fn({
                        request,
                        params
                    });
                    expect(actual.status).toEqual(200);
                    expect(actual.headers.get("X-BM-FromCache")).toEqual(JSON.stringify(true));
                });

                test("fallback to network on Error", async () => {
                    maildb.getInstance.mockReturnValue({
                        isSubscribed: () => true
                    });
                    const actual = await fn({
                        request,
                        params
                    });
                    expect(actual.status).toEqual(200);
                    expect(actual.headers.get("X-BM-FromCache")).toBeNull();
                    expect(await actual.json()).toEqual(["foo"]);
                });
            });
        });
    });
});
