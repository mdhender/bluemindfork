import { updateAttachment } from "../../../MessageStore/mutations/updateAttachment";

const state = {
    parts: {
        attachments: [
            { uid: "id1", propOne: "att1P1", propTwo: "att1P2" },
            { uid: "id2", propOne: "att2P1", propTwo: "att2P2" }
        ]
    }
};

describe("[Mail-WebappStore/MessageStore][mutations] : updateAttachment ", () => {
    test("Basic", () => {
        updateAttachment(state, {
            uid: "id1",
            propOne: "att1P1",
            propTwo: "att1P2-modified",
            propThree: "newProp"
        });
        expect(state.parts.attachments).toEqual([
            {
                uid: "id1",
                propOne: "att1P1",
                propTwo: "att1P2-modified",
                propThree: "newProp"
            },
            { uid: "id2", propOne: "att2P1", propTwo: "att2P2" }
        ]);
    });
});
