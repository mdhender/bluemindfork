import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import store from "../messageCompose";

Vue.use(Vuex);

const file1 = {
    key: 1,
    fileName: "image.jpg",
    size: 100
};

let state;

describe("files", () => {
    beforeEach(() => {
        state = new Vuex.Store(cloneDeep(store)).state;
    });
    describe("ADD", () => {
        test("ADD_FILE", () => {
            store.mutations["ADD_FILE"](state, { file: file1 });
            expect(state.uploadingFiles[file1.key]).toEqual(file1);
        });
    });
    describe("File properties", () => {
        beforeEach(() => {
            store.mutations["ADD_FILE"](state, { file: file1 });
        });
        test("SET_FILE_PROGRESS", () => {
            store.mutations["SET_FILE_PROGRESS"](state, { key: file1.key, progress: { loaded: 500, total: 10000 } });
            expect(state.uploadingFiles[file1.key]).toEqual(
                expect.objectContaining({ progress: { loaded: 500, total: 10000 } })
            );
        });
        test("SET_FILE_STATUS", () => {
            const status = "STATUS";
            store.mutations["SET_FILE_STATUS"](state, { key: file1.key, status });
            expect(state.uploadingFiles[file1.key]).toEqual(expect.objectContaining({ status }));
        });
        test("SET_FILE_ADDRESS", () => {
            const address = "1234";
            store.mutations["SET_FILE_ADDRESS"](state, { key: file1.key, address });
            expect(state.uploadingFiles[file1.key]).toEqual(expect.objectContaining({ address }));
        });
        test("SET_FILE_HEADERS", () => {
            const headers = [
                { name: "header1", value: "value1" },
                { name: "header2", value: "value2" }
            ];
            store.mutations["SET_FILE_HEADERS"](state, { key: file1.key, headers });
            expect(state.uploadingFiles[file1.key]).toEqual(expect.objectContaining({ headers }));
        });
    });
});
