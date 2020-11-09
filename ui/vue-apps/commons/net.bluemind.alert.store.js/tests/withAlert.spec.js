import { ERROR, LOADING, SUCCESS } from "../src";
import { withAlert } from "../src/withAlert";

const ROOT = { root: true };
describe("withAlert", () => {
    let store = {
        commit: jest.fn(),
        dispatch: jest.fn()
    };
    const payload = { data: "" };
    const result = "Result";
    const action = jest.fn().mockResolvedValue(result);
    beforeEach(() => {
        store.commit.mockClear();
        store.dispatch.mockClear();
        action.mockClear();
    });
    test("LOADING", () => {
        const actionWithAlert = withAlert(action, "ActionName", "Renderer");
        actionWithAlert(store, payload);
        expect(store.commit).toHaveBeenCalledWith(
            "alert/" + LOADING,
            { name: "ActionName", payload, renderer: "Renderer", uid: expect.anything() },
            ROOT
        );
    });
    test("SUCCESS", async () => {
        const actionWithAlert = withAlert(action, "ActionName", "Renderer");
        const value = await actionWithAlert(store, payload);
        expect(store.commit).toHaveBeenCalledWith(
            "alert/" + SUCCESS,
            {
                name: "ActionName",
                payload,
                result,
                renderer: "Renderer",
                uid: expect.anything()
            },
            ROOT
        );
        expect(value).toBe(result);
        expect(store.commit).not.toHaveBeenCalledWith("alert/" + ERROR, expect.anything());
    });
    test("ERROR", async () => {
        const error = "ERROR";
        action.mockRejectedValueOnce(error);
        const actionWithAlert = withAlert(action, "ActionName", "Renderer");
        try {
            await actionWithAlert(store, payload);
            throw new Error("action should have throw an exception");
        } catch (error) {
            expect(store.commit).toHaveBeenCalledWith(
                "alert/" + ERROR,
                {
                    name: "ActionName",
                    payload,
                    error,
                    renderer: "Renderer",
                    uid: expect.anything()
                },
                ROOT
            );
            expect(store.commit).not.toHaveBeenCalledWith("alert/" + SUCCESS, expect.anything());
        }
    });
    test("Alert uid must be the same for loading and success", async () => {
        let currentUid;
        store.commit.mockImplementation((dummy, { uid }) => {
            if (!currentUid) currentUid = uid;
        });
        await withAlert(action, "ActionName", "Renderer")(store, payload);
        expect(store.commit).toHaveBeenCalledWith(
            "alert/" + LOADING,
            expect.objectContaining({ uid: currentUid }),
            ROOT
        );
        expect(store.commit).toHaveBeenCalledWith(
            "alert/" + SUCCESS,
            expect.objectContaining({ uid: currentUid }),
            ROOT
        );
    });
    test("Alert uid must be the same for loading and error", async () => {
        let currentUid;
        store.commit.mockImplementation((dummy, { uid }) => {
            if (!currentUid) currentUid = uid;
        });
        action.mockRejectedValueOnce();
        try {
            await withAlert(action, "ActionName", "Renderer")(store, payload);
            throw new Error("action should have throw an exception");
        } catch (error) {
            expect(store.commit).toHaveBeenCalledWith(
                "alert/" + LOADING,
                expect.objectContaining({ uid: currentUid }),
                ROOT
            );
            expect(store.commit).toHaveBeenCalledWith(
                "alert/" + ERROR,
                expect.objectContaining({ uid: currentUid }),
                ROOT
            );
        }
    });
    test("Alert uid must be unique per action execution", async () => {
        let currentUid;
        store.commit.mockImplementation((dummy, { uid }) => {
            if (!currentUid) currentUid = uid;
        });
        withAlert(action, "ActionName", "Renderer")(store, payload);
        withAlert(action, "ActionName", "Renderer")(store, payload);
        expect(store.commit).toHaveBeenNthCalledWith(
            2,
            "alert/" + LOADING,
            expect.not.objectContaining({ uid: currentUid }),
            ROOT
        );
    });
    test("Expect name to fallback on action.name", () => {
        let action = () => {};
        withAlert(action)(store);

        expect(store.commit).toHaveBeenCalledWith(
            "alert/" + LOADING,
            expect.objectContaining({ name: "action" }),
            ROOT
        );
    });
});
