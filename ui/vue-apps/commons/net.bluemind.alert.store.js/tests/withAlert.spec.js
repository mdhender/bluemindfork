import UUIDGenerator from "@bluemind/uuid";
import { ERROR, LOADING, SUCCESS } from "../src";
import { withAlert } from "../src/withAlert";

jest.mock("@bluemind/uuid");

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
        UUIDGenerator.generate.mockReturnValue(Math.random());
    });
    test("LOADING", () => {
        const actionWithAlert = withAlert(action, "ActionName", { renderer: "Renderer" });
        actionWithAlert(store, payload);
        expect(store.dispatch).toHaveBeenCalledWith(
            "alert/" + LOADING,
            {
                alert: { name: "ActionName", payload, uid: expect.anything() },
                options: { renderer: "Renderer" }
            },
            ROOT
        );
    });
    test("SUCCESS", async () => {
        const actionWithAlert = withAlert(action, "ActionName", { renderer: "Renderer" });
        const value = await actionWithAlert(store, payload);
        expect(store.dispatch).toHaveBeenCalledWith(
            "alert/" + SUCCESS,
            {
                alert: {
                    name: "ActionName",
                    payload,
                    result,
                    uid: expect.anything()
                },
                options: { renderer: "Renderer" }
            },
            ROOT
        );
        expect(value).toBe(result);
        expect(store.dispatch).not.toHaveBeenCalledWith("alert/" + ERROR, expect.anything());
    });
    test("ERROR", async () => {
        const error = "ERROR";
        action.mockRejectedValueOnce(error);
        const actionWithAlert = withAlert(action, "ActionName", { renderer: "Renderer" });
        try {
            await actionWithAlert(store, payload);
            throw new Error("action should have throw an exception");
        } catch (error) {
            expect(store.dispatch).toHaveBeenCalledWith(
                "alert/" + ERROR,
                {
                    alert: { name: "ActionName", payload, error, uid: expect.anything() },
                    options: { renderer: "Renderer" }
                },
                ROOT
            );
            expect(store.commit).not.toHaveBeenCalledWith("alert/" + SUCCESS, expect.anything());
        }
    });
    test("Alert uid must be the same for loading and success", async () => {
        const currentUid = "UID";
        UUIDGenerator.generate.mockReturnValueOnce(currentUid);
        await withAlert(action, "ActionName", "Renderer")(store, payload);
        expect(store.dispatch).toHaveBeenCalledWith(
            "alert/" + LOADING,
            expect.objectContaining({ alert: expect.objectContaining({ uid: currentUid }) }),
            ROOT
        );
        expect(store.dispatch).toHaveBeenCalledWith(
            "alert/" + SUCCESS,
            expect.objectContaining({ alert: expect.objectContaining({ uid: currentUid }) }),
            ROOT
        );
    });
    test("Alert uid must be the same for loading and error", async () => {
        const currentUid = "UID";
        UUIDGenerator.generate.mockReturnValueOnce(currentUid);
        action.mockRejectedValueOnce();
        try {
            await withAlert(action, "ActionName", "Renderer")(store, payload);
            throw new Error("action should have throw an exception");
        } catch (error) {
            expect(store.dispatch).toHaveBeenCalledWith(
                "alert/" + LOADING,
                expect.objectContaining({ alert: expect.objectContaining({ uid: currentUid }) }),
                ROOT
            );
            expect(store.dispatch).toHaveBeenCalledWith(
                "alert/" + ERROR,
                expect.objectContaining({ alert: expect.objectContaining({ uid: currentUid }) }),
                ROOT
            );
        }
    });
    test("Alert uid must be unique per action execution", async () => {
        const currentUid = "UID";
        UUIDGenerator.generate.mockReturnValueOnce(currentUid);
        withAlert(action, "ActionName", "Renderer")(store, payload);
        withAlert(action, "ActionName", "Renderer")(store, payload);
        expect(store.dispatch).toHaveBeenNthCalledWith(
            2,
            "alert/" + LOADING,
            expect.not.objectContaining({ alert: expect.objectContaining({ uid: currentUid }) }),
            ROOT
        );
    });
    test("Expect name to fallback on action.name", () => {
        let action = () => {};
        withAlert(action)(store);

        expect(store.dispatch).toHaveBeenCalledWith(
            "alert/" + LOADING,
            expect.objectContaining({ alert: expect.objectContaining({ name: "action" }) }),
            ROOT
        );
    });
});
