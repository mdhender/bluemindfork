import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import store from "../files";

Vue.use(Vuex);

const file1 = {
    key: 1,
    fileName: "image.jpg",
    size: 100
};
const file2 = {
    key: 2,
    fileName: "audio.mp3",
    size: 300
};
const files = [file1, file2];
let state;

describe("files", () => {
    beforeEach(() => {
        state = new Vuex.Store(cloneDeep(store));
    });
    describe("ADD and REMOVE", () => {
        test("ADD_FILE", () => {
            store.mutations["ADD_FILE"](state, { file: file1 });
            expect(state[file1.key]).toEqual(file1);
        });
        test("ADD_FILES", () => {
            store.mutations["ADD_FILES"](state, { files });
            expect(state[file1.key]).toEqual(file1);
            expect(state[file2.key]).toEqual(file2);
        });
        test("REMOVE_FILE", () => {
            store.mutations["ADD_FILE"](state, { file: file1 });
            store.mutations["REMOVE_FILE"](state, { key: file1.key });
            expect(state[file2.key]).toBeFalsy();
        });
    });
    describe("File properties", () => {
        beforeEach(() => {
            store.mutations["ADD_FILE"](state, { file: file1 });
        });
        test("SET_FILE_PROGRESS", () => {
            store.mutations["SET_FILE_PROGRESS"](state, { key: file1.key, loaded: 500, total: 10000 });
            expect(state[file1.key]).toEqual(expect.objectContaining({ progress: { loaded: 500, total: 10000 } }));
        });
        test("SET_FILE_STATUS", () => {
            const status = "STATUS";
            store.mutations["SET_FILE_STATUS"](state, { key: file1.key, status });
            expect(state[file1.key]).toEqual(expect.objectContaining({ status }));
        });
        test("SET_FILE_URL", () => {
            const url = "www.url.com";
            store.mutations["SET_FILE_URL"](state, { key: file1.key, url });
            expect(state[file1.key]).toEqual(expect.objectContaining({ url }));
        });
        test("SET_FILE_ADDRESS", () => {
            const address = "1234";
            store.mutations["SET_FILE_ADDRESS"](state, { key: file1.key, address });
            expect(state[file1.key]).toEqual(expect.objectContaining({ address }));
        });
        test("SET_FILE_HEADERS", () => {
            const headers = [
                { name: "header1", value: "value1" },
                { name: "header2", value: "value2" }
            ];
            store.mutations["SET_FILE_HEADERS"](state, { key: file1.key, headers });
            expect(state[file1.key]).toEqual(expect.objectContaining({ headers }));
        });
    });
});
