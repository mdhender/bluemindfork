import MailboxItemsDBProxy from "../proxies/MailboxItemsDBProxy";
import { MailboxItem, MailboxItemsClient } from "@bluemind/backend.mail.api";
import db, { MailDB } from "../MailDB";
import { ItemFlag } from "@bluemind/core.container.api";

jest.mock("../MailDB");

describe("MailboxItemsDBProxy", () => {
    afterEach(() => {
        jest.restoreAllMocks();
    });
    describe("multipleGetById", () => {
        test("multipleGetById fallback to server if some items are not found", async () => {
            const MailboxItemsClientSpy = {
                multipleGetById: jest.spyOn(MailboxItemsClient.prototype, "multipleGetById").mockResolvedValue([])
            };
            const proxy = new MailboxItemsDBProxy("api-key", "mailboxy-uid");
            const fakeItem: MailboxItem = { body: {} };
            (db as jest.Mocked<MailDB>).isSubscribed.mockResolvedValueOnce(true);
            (db as jest.Mocked<MailDB>).getMailItems.mockResolvedValueOnce([
                { internalId: 1, value: fakeItem },
                { internalId: 3, value: fakeItem }
            ]);
            await proxy.multipleGetById([1, 2, 3, 4]);
            expect(MailboxItemsClientSpy.multipleGetById).toHaveBeenCalledWith([2, 4]);
        });
        test("server result are added to multipleGetById result", async () => {
            const assert = [{ internalId: 2, value: { body: {} } }];
            jest.spyOn(MailboxItemsClient.prototype, "multipleGetById").mockResolvedValue(assert);
            const proxy = new MailboxItemsDBProxy("api-key", "mailboxy-uid");
            (db as jest.Mocked<MailDB>).isSubscribed.mockResolvedValueOnce(true);
            (db as jest.Mocked<MailDB>).getMailItems.mockResolvedValueOnce([{ internalId: 1, value: { body: {} } }]);
            const result = await proxy.multipleGetById([1, 2]);
            expect(result).toEqual(expect.arrayContaining(assert));
        });
        test("multipleGetById do not fallback to server if all items are found", async () => {
            const MailboxItemsClientSpy = {
                multipleGetById: jest.spyOn(MailboxItemsClient.prototype, "multipleGetById")
            };
            const proxy = new MailboxItemsDBProxy("api-key", "mailboxy-uid");
            const fakeItem: MailboxItem = { body: {} };
            (db as jest.Mocked<MailDB>).isSubscribed.mockResolvedValueOnce(true);
            (db as jest.Mocked<MailDB>).getMailItems.mockResolvedValueOnce([
                { internalId: 1, value: fakeItem },
                { internalId: 3, value: fakeItem }
            ]);
            await proxy.multipleGetById([1, 3]);
            expect(MailboxItemsClientSpy.multipleGetById).not.toHaveBeenCalled();
        });
    });
    describe("sortedId", () => {
        test("sortedId fallback to server filter is not supported by MailDb", async () => {
            const proxy = new MailboxItemsDBProxy("api-key", "mailboxy-uid");
            proxy.next = jest.fn();
            await proxy.sortedIds({ filter: { must: [ItemFlag.Deleted] } });
            (db as jest.Mocked<MailDB>).isSubscribed.mockResolvedValueOnce(true);
            expect(proxy.next).toHaveBeenCalled();
        });
        test("sortedId not to fallback to server filter is supported by MailDb", async () => {
            const proxy = new MailboxItemsDBProxy("api-key", "mailboxy-uid");
            proxy.next = jest.fn();
            await proxy.sortedIds({ filter: { mustNot: [ItemFlag.Deleted] } });
            (db as jest.Mocked<MailDB>).isSubscribed.mockResolvedValueOnce(true);
            expect(proxy.next).not.toHaveBeenCalled();
        });
    });
});
