import { update } from "../../../src/MessageStore/mutations/update";

const state = {
    key: "AD1DZSD4",
    id: "42",
    parts: {
        attachments: ["att01", "att02"],
        inlines: ["inl01", "inl02"]
    },
    saveDate: "date",
    status: "status"
};

describe("[Mail-WebappStore/MessageStore][mutations] : update ", () => {
    test("Basic", () => {
        update(state, {
            key: "newkey",
            id: "newid",
            parts: {
                attachments: ["newatt01", "newatt02"],
                inlines: ["newinl01", "newinl02"]
            },
            saveDate: "newdate",
            status: "newstatus"
        });
        expect(state.key).toBe("newkey");
        expect(state.id).toBe("newid");
        expect(state.parts.attachments).toStrictEqual(["newatt01", "newatt02"]);
        expect(state.parts.inlines).toStrictEqual(["newinl01", "newinl02"]);
        expect(state.saveDate).toBe("newdate");
        expect(state.status).toBe("newstatus");
    });
});
