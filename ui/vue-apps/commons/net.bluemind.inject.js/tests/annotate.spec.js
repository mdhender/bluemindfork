import { annotate } from "../src/annotate";

describe("annotate", () => {
    test("Annotated function have a $inject property", () => {
        const fn = () => {};
        annotate(fn);
        expect(fn.$inject).toBeDefined();
    });

    test("Annotate return value and $inject function property are the same", () => {
        const fn = () => {};
        const injected = annotate(fn);
        expect(fn.$inject).toEqual(injected);
    });

    test("Annotate return an array with function all parameters name", () => {
        const fn = (param1, param2) => {
            return param1 + param2;
        };
        const injected = annotate(fn);
        expect(injected).toEqual(["param1", "param2"]);
    });

    test("Annotate return an array with function parameters name", () => {
        const fn = param => {
            return param;
        };
        const injected = annotate(fn);
        expect(injected[0]).toEqual("param");
    });

    test("Annotate support comments", () => {
        const fn = (param /*not null*/) => param;
        const injected = annotate(fn);
        expect(injected[0]).toEqual("param");
    });

    test("Annotate does not support default value", () => {
        const fn = (param = 3) => param;
        const injected = annotate(fn);
        expect(injected[0]).not.toBe("param");
    });

    test("Annotate support constructor method", () => {
        class A {
            constructor(a, b, c) {
                a + b + c;
            }
        }
        const injected = annotate(A.prototype.constructor);
        expect(injected).toEqual(["a", "b", "c"]);
    });

    test("Annotate support inline function", () => {
        let injected = annotate(param => param);
        expect(injected).toEqual(["param"]);
        injected = annotate(param => {
            param;
        });
        injected = annotate((param1, param2) => param1 || param2);
        expect(injected).toEqual(["param1", "param2"]);
        injected = annotate((param1, param2) => {
            param1 || param2;
        });
        expect(injected).toEqual(["param1", "param2"]);
    });

    test("Functiton without parameter have an empty annotation array", () => {
        let injected = annotate(() => {});
        expect(injected.length).toEqual(0);
        injected = annotate(() => 0);
        expect(injected.length).toEqual(0);
    });
});
