import { currentFolder } from "../../src/getters/currentFolder";

const getters = {
    "folders/getFolderByKey": jest.fn().mockReturnValue("TheFolder")
};

const state = { currentFolderKey: "key2" };

describe("[Mail-WebappStore][getters] : currentFolder ", () => {
    test("return current Folder instance ", () => {
        const result = currentFolder(state, getters);
        expect(getters["folders/getFolderByKey"]).toHaveBeenCalledWith("key2");
        expect(result).toEqual("TheFolder");
    });
});
