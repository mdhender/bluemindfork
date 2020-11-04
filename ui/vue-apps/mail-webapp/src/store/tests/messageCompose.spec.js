import inject from "@bluemind/inject";
import { MockUserIdentitiesClient } from "@bluemind/test-utils";

import { actions, state, mutations } from "../messageCompose";
import { SET_SIGNATURE } from "../types/mutations";

const mySignature = "My Signature";

describe("messageCompose store", () => {
    describe("mutations", () => {
        test("SET_SIGNATURE", () => {
            expect(state.messageCompose.signature).toBe("");
            mutations.SET_SIGNATURE(state, mySignature);
            expect(state.messageCompose.signature).toBe(mySignature);
        });
    });

    describe("actions", () => {
        const context = {
            state,
            commit: jest.fn()
        };

        const userIdentitiesService = new MockUserIdentitiesClient();
        inject.register({ provide: "IUserMailIdentities", factory: () => userIdentitiesService });
        userIdentitiesService.getIdentities.mockReturnValue([
            { isDefault: false },
            { isDefault: true, signature: mySignature }
        ]);

        test("FETCH_SIGNATURE", async () => {
            await actions.FETCH_SIGNATURE(context);
            expect(userIdentitiesService.getIdentities).toHaveBeenCalled();
            expect(context.commit).toHaveBeenCalledWith(SET_SIGNATURE, mySignature);
        });
    });
});
