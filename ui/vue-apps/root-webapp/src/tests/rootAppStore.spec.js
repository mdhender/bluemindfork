import inject from "@bluemind/inject";
import rootAppStore from "../rootAppStore";
import { MockMailboxesClient } from "@bluemind/test-utils";

const userId = "user:id";
const mailboxesClient = new MockMailboxesClient();
inject.register({ provide: "MailboxesPersistence", factory: () => mailboxesClient });
inject.register({ provide: "UserSession", factory: () => ({ userId }) });

describe("Store rootApp", () => {
    let context;

    beforeEach(() => {
        context = {
            state: { quota: {} },
            commit: jest.fn()
        };
    });

    test("FETCH_MY_MAILBOX_QUOTA action", async () => {
        mailboxesClient.getMailboxQuota.mockReturnValue({ used: 4, quota: 10 });

        await rootAppStore.actions.FETCH_MY_MAILBOX_QUOTA(context);
        expect(mailboxesClient.getMailboxQuota).toHaveBeenCalledWith(userId);
        expect(context.commit).toHaveBeenCalledWith("SET_QUOTA", { used: 4, total: 10 });
    });

    test("SET_QUOTA mutation", async () => {
        const quota = { used: 4, total: 10 };
        rootAppStore.mutations.SET_QUOTA(context.state, quota);
        expect(context.state.quota).toEqual(quota);
    });

    test("SET_APP_STATE mutation", async () => {
        rootAppStore.mutations.SET_APP_STATE(context.state, "anything");
        expect(context.state.appState).toEqual("anything");
    });
});
