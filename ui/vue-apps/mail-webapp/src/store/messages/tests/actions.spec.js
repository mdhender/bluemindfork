import Vue from "vue";
import Vuex from "vuex";
import deepClone from "lodash.clonedeep";

import api from "../../api/apiMessages";
import storeConfig from "../../messages";
import { ADD_MESSAGE } from "../mutations";
import { FETCH_MESSAGES, MARK_AS_READ } from "../actions";

Vue.use(Vuex);
jest.mock("../../api/apiMessages");

describe("messages", () => {
    describe("actions", () => {
        test("FETCH_MESSAGES", async () => {
            const store = new Vuex.Store(deepClone(storeConfig));
            const folderUid = "foo";
            api.fetchMessages.mockImplementation(({ folderUid: uid }) => {
                expect(uid).toBe(folderUid);
                return [{ uid: 1 }];
            });
            await store.dispatch(FETCH_MESSAGES, { folderUid });
            expect(store.state).toMatchInlineSnapshot(`
                Object {
                  "1": Object {
                    "folder": "foo",
                    "key": 1,
                    "uid": 1,
                  },
                }
            `);
        });
        test("MARK_AS_READ", async () => {
            const store = new Vuex.Store(deepClone(storeConfig));
            const message = {
                key: 1,
                folder: 1,
                data: { flags: { read: false } }
            };
            store.commit(ADD_MESSAGE, message);
            api.markAsRead = jest.fn();
            await store.dispatch(MARK_AS_READ, message);
            expect(store.state).toMatchInlineSnapshot(`
                Object {
                  "1": Object {
                    "data": Object {
                      "flags": Object {
                        "read": true,
                      },
                    },
                    "folder": 1,
                    "key": 1,
                  },
                }
            `);
        });
        test("MARK_AS_READ failing", async () => {
            const store = new Vuex.Store(deepClone(storeConfig));
            const message = {
                key: 1,
                folder: 1,
                data: { flags: { read: false } }
            };
            store.commit(ADD_MESSAGE, message);
            api.markAsRead = jest.fn();
            await store.dispatch(MARK_AS_READ, message);
            expect(store.state).toMatchInlineSnapshot(`
                Object {
                  "1": Object {
                    "data": Object {
                      "flags": Object {
                        "read": true,
                      },
                    },
                    "folder": 1,
                    "key": 1,
                  },
                }
            `);
        });
    });
});
