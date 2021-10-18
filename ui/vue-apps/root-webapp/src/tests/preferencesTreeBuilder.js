import { merge, reactive, sanitize } from "../components/preferences/sections/builder";
import { RoleCondition, StoreFieldCondition } from "../components/preferences/conditions";
jest.mock("../components/preferences/conditions");

describe("PreferenceSections", () => {
    describe("merge", () => {
        test("normalize zone before merge", () => {
            let section = { id: "section.category" };
            expect(merge([], section)).toEqual([{ id: "section", categories: [{ id: "category" }] }]);
            section = { id: "section.category.group" };
            expect(merge([], section)).toEqual([
                { id: "section", categories: [{ id: "category", groups: [{ id: "group" }] }] }
            ]);
            section = { id: "section.category.group.field" };
            expect(merge([], section)).toEqual([
                {
                    id: "section",
                    categories: [{ id: "category", groups: [{ id: "group", fields: [{ id: "field" }] }] }]
                }
            ]);
        });
        test("add section if not already present", () => {
            const one = { id: "one" };
            const two = { id: "two" };
            expect(merge([one], two)).toEqual([one, two]);
        });

        test("merge section data if already exist", () => {
            const origin = { id: "one", fromOrigin: "origin", conflict: "origin" };
            const extension = { id: "one", fromExtension: "extension", conflict: "extension" };
            expect(merge([origin], extension)).toEqual([
                { id: "one", fromOrigin: "origin", fromExtension: "extension", conflict: "extension" }
            ]);
        });
        test("add category if not already present", () => {
            const origin = { id: "section", categories: [{ id: "category" }] };
            const extension = { id: "section.category2" };
            expect(merge([origin], extension)).toEqual([
                {
                    id: "section",
                    categories: [{ id: "category" }, { id: "category2" }]
                }
            ]);
        });
        test("merge category data if already exist", () => {
            const origin = {
                id: "section",
                categories: [{ id: "category", fromOrigin: "origin", conflict: "origin" }]
            };
            const extension = { id: "section.category", fromExtension: "extension", conflict: "extension" };
            expect(merge([origin], extension)).toEqual([
                {
                    id: "section",
                    categories: [
                        { id: "category", fromOrigin: "origin", fromExtension: "extension", conflict: "extension" }
                    ]
                }
            ]);
        });
        test("add group if not already present", () => {
            const origin = { id: "section", categories: [{ id: "category", groups: [{ id: "group" }] }] };
            const extension = { id: "section.category.group2" };
            expect(merge([origin], extension)).toEqual([
                {
                    id: "section",
                    categories: [{ id: "category", groups: [{ id: "group" }, { id: "group2" }] }]
                }
            ]);
        });
        test("merge group data if already exist", () => {
            const origin = {
                id: "section",
                categories: [{ id: "category", groups: [{ id: "group", fromOrigin: "origin", conflict: "origin" }] }]
            };
            const extension = { id: "section.category.group", fromExtension: "extension", conflict: "extension" };
            expect(merge([origin], extension)).toEqual([
                {
                    id: "section",
                    categories: [
                        {
                            id: "category",
                            groups: [
                                { id: "group", fromOrigin: "origin", fromExtension: "extension", conflict: "extension" }
                            ]
                        }
                    ]
                }
            ]);
        });
        test("add field if not already present", () => {
            const origin = {
                id: "section",
                categories: [{ id: "category", groups: [{ id: "group", fields: [{ id: "field" }] }] }]
            };
            const extension = { id: "section.category.group.field2" };
            expect(merge([origin], extension)).toEqual([
                {
                    id: "section",
                    categories: [
                        { id: "category", groups: [{ id: "group", fields: [{ id: "field" }, { id: "field2" }] }] }
                    ]
                }
            ]);
        });
        test("merge field data if already exist", () => {
            const origin = {
                id: "section",
                categories: [
                    {
                        id: "category",
                        groups: [{ id: "group", fields: [{ id: "field", fromOrigin: "origin", conflict: "origin" }] }]
                    }
                ]
            };
            const extension = { id: "section.category.group.field", fromExtension: "extension", conflict: "extension" };
            expect(merge([origin], extension)).toEqual([
                {
                    id: "section",
                    categories: [
                        {
                            id: "category",
                            groups: [
                                {
                                    id: "group",
                                    fields: [
                                        {
                                            id: "field",
                                            fromOrigin: "origin",
                                            fromExtension: "extension",
                                            conflict: "extension"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]);
        });
    });
    describe("sanitize", () => {
        test("make id unique", () => {
            const preferences = [
                {
                    id: "section_1",
                    categories: [
                        {
                            id: "category_1",
                            groups: [
                                { id: "group_1", fields: [{ id: "field_1" }] },
                                { id: "group_2", field: [{ id: "field_1" }] }
                            ]
                        },
                        { id: "category_2", groups: [{ id: "group_1", fields: [] }] }
                    ]
                },
                { id: "section_2", categories: [{ id: "category_1", groups: [] }] }
            ];

            const ids = new Set();
            const findDuplicate = (hasDuplicate, { id, categories, groups, fields }) => {
                if (hasDuplicate || ids.has(id)) {
                    return true;
                }
                ids.add(id);
                return (categories || groups || fields || []).reduce(findDuplicate, false);
            };
            const hasDuplicate = sanitize(preferences).reduce(findDuplicate, false);
            expect(hasDuplicate).toBeFalsy();
        });
        test("add a priority to all zone without one", () => {
            const preferences = [
                {
                    id: "section_1",
                    categories: [
                        {
                            id: "category_1",
                            groups: [{ id: "group_1", fields: [{ id: "field_1" }] }]
                        }
                    ]
                },
                { id: "section_2", priority: 100 }
            ];
            const checkPriority = (hasPriority, { priority, categories, groups, fields }) => {
                if (!hasPriority || !priority) {
                    return false;
                }
                return (categories || groups || fields || []).reduce(checkPriority, true);
            };
            const hasPriority = sanitize(preferences).reduce(checkPriority, true);
            expect(hasPriority).toBeTruthy();
        });
        test("Set visible to true if not set", () => {
            const preferences = [
                {
                    id: "1",
                    categories: []
                },
                { id: "2", visible: false, categories: [] }
            ];
            const sanitized = sanitize(preferences);
            expect(sanitized[0].visible).toBeTruthy();
            expect(sanitized[1].visible).toBeFalsy();
        });
        test("Set disabled to false if not set", () => {
            const preferences = [
                {
                    id: "1",
                    categories: []
                },
                { id: "2", disabled: true, categories: [] }
            ];
            const sanitized = sanitize(preferences);
            expect(sanitized[0].disabled).toBeFalsy();
            expect(sanitized[1].disabled).toBeTruthy();
        });
        test("Transform visible and disabled to boolean", () => {
            const preferences = [
                {
                    id: "1",
                    visible: "false",
                    categories: [
                        {
                            id: "2",
                            visible: "true",
                            groups: [
                                {
                                    id: "3",
                                    disabled: 0,
                                    fields: [
                                        {
                                            id: "4",
                                            disabled: 1
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ];
            const sanitized = sanitize(preferences);
            expect(sanitized[0].visible).toBeFalsy();
            expect(sanitized[0].categories[0].visible).toBeTruthy();
            expect(sanitized[0].categories[0].groups[0].disabled).toBeFalsy();
            expect(sanitized[0].categories[0].groups[0].fields[0].disabled).toBeTruthy();
        });

        test("Keep visible and disabled as function", () => {
            const preferences = [
                {
                    id: "1",
                    categories: [],
                    visible: Function(),
                    disabled: Function()
                },
                { id: "2", categories: [], visible: 0, disabled: 1 }
            ];
            const sanitized = sanitize(preferences);
            expect(typeof sanitized[0].visible).toEqual("function");
            expect(typeof sanitized[0].disabled).toEqual("function");
        });
        test("To call RoleCondition for visible/disabled when asked", () => {
            const preferences = [
                {
                    id: "1",
                    visible: { name: "RoleCondition", args: ["1"] },
                    categories: [
                        {
                            id: "2",
                            visible: { name: "RoleCondition.some", args: ["2"] },
                            groups: [
                                {
                                    id: "3",
                                    disabled: { name: "RoleCondition.every", args: ["3"] },
                                    fields: [
                                        {
                                            id: "4",
                                            disabled: { name: "RoleCondition.none", args: ["4"] }
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ];
            sanitize(preferences);

            expect(RoleCondition).toHaveBeenCalledWith("1");
            expect(RoleCondition.some).toHaveBeenCalledWith("2");
            expect(RoleCondition.every).toHaveBeenCalledWith("3");
            expect(RoleCondition.none).toHaveBeenCalledWith("4");
        });
        test("To call StoreFieldCondition for visible/disabled when asked", () => {
            const preferences = [
                {
                    id: "1",
                    visible: { name: "StoreFieldCondition", args: ["1"] },
                    categories: [
                        {
                            id: "2",
                            visible: { name: "StoreFieldCondition.current", args: ["2"] },
                            groups: [
                                {
                                    id: "3",
                                    disabled: { name: "StoreFieldCondition.saved", args: ["3"] },
                                    fields: []
                                }
                            ]
                        }
                    ]
                }
            ];

            sanitize(preferences);

            expect(StoreFieldCondition).toHaveBeenCalledWith("1");
            expect(StoreFieldCondition.current).toHaveBeenCalledWith("2");
            expect(StoreFieldCondition.saved).toHaveBeenCalledWith("3");
        });
        test("To transform condition arguments into a function", () => {
            const preferences = [
                {
                    id: "1",
                    visible: { name: "Function", args: ["arg", "return arg"] },
                    categories: [
                        {
                            id: "2",
                            visible: "return true;",
                            groups: [
                                {
                                    id: "3",
                                    disabled: ["num", "return num + 1"],
                                    fields: []
                                }
                            ]
                        }
                    ]
                }
            ];
            const sanitized = sanitize(preferences);
            expect(typeof sanitized[0].visible).toEqual("function");
            expect(sanitized[0].visible("argument")).toEqual("argument");
            expect(typeof sanitized[0].categories[0].visible).toEqual("function");
            expect(sanitized[0].categories[0].visible()).toBeTruthy();
            expect(typeof sanitized[0].categories[0].groups[0].disabled).toEqual("function");
            expect(sanitized[0].categories[0].groups[0].disabled(1)).toBe(2);
        });
        test("to sort section by priority", () => {
            let preferences = [
                { id: "3", priority: 0 },
                { id: "2" },
                { id: "1", priority: 10 },
                {
                    id: "0",
                    priority: Number.MAX_SAFE_INTEGER,
                    categories: [
                        { id: "3", priority: 0 },
                        { id: "2" },
                        { id: "1", priority: 10 },
                        {
                            id: "0",
                            priority: Number.MAX_SAFE_INTEGER,
                            groups: [
                                { id: "3", priority: 0 },
                                { id: "2" },
                                { id: "1", priority: 10 },
                                {
                                    id: "0",
                                    priority: Number.MAX_SAFE_INTEGER,
                                    fields: [
                                        { id: "3", priority: 0 },
                                        { id: "2" },
                                        { id: "1", priority: 10 },
                                        { id: "0", priority: Number.MAX_SAFE_INTEGER }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ];
            let sanitized = sanitize(preferences);
            expect(sanitized.every(({ id }, index) => parseInt(id) === index)).toBeTruthy();
            expect(
                sanitized[0].categories.every(({ id }, index) => parseInt(id.split(".").pop()) === index)
            ).toBeTruthy();
            expect(
                sanitized[0].categories[0].groups.every(({ id }, index) => parseInt(id.split(".").pop()) === index)
            ).toBeTruthy();
            expect(
                sanitized[0].categories[0].groups[0].fields.every(
                    ({ id }, index) => parseInt(id.split(".").pop()) === index
                )
            ).toBeTruthy();
        });
    });
    describe("reactive", () => {
        test("Make a condition function reactive", () => {
            const fn = Function();
            let preferences = [{ id: "3", visible: fn }];
            const vm = { $watch: jest.fn() };
            reactive(preferences, vm);
            expect(vm.$watch).toHaveBeenCalledWith(fn, expect.anything(), expect.anything());
        });
    });
});
