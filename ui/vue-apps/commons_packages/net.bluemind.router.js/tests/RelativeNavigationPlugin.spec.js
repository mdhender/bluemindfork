import RelativeNavigationPlugin from "../src/RelativeNavigationPlugin";

describe("RelativeNavigationPlugin", () => {
    test("Add navigate and relative method to router", () => {
        const mock = function () {};
        RelativeNavigationPlugin.extends(mock);
        expect(mock.prototype.navigate).toBeDefined();
        expect(mock.prototype.relative).toBeDefined();
        mock.prototype.navigate = "Do not overwrite";
        mock.prototype.relative = "Do not overwrite";
        RelativeNavigationPlugin.extends(mock);
        expect(mock.prototype.navigate).toEqual("Do not overwrite");
        expect(mock.prototype.relative).toEqual("Do not overwrite");
    });
    test("Relative keep all not overwritten params", () => {
        const mock = function () {};
        mock.prototype.push = jest.fn();
        RelativeNavigationPlugin.extends(mock);
        const router = new mock();

        let current = {
            name: "mail-home",
            params: { folder: "INBOX" }
        };
        let location = router.relative({ name: "mail-message", params: { message: "3." } }, current);
        expect(location.name).toEqual("mail-message");
        expect(location.params).toEqual({ folder: "INBOX", message: "3." });
        current = {
            name: "mail-reply",
            params: { folder: "INBOX", message: "3." }
        };
        location = router.relative({ name: "mail-message", params: { message: "5." } }, current);
        expect(location.name).toEqual("mail-message");
        expect(location.params).toEqual({ folder: "INBOX", message: "5." });
    });
    test("Relative accept a string as route name", () => {
        const mock = function () {};
        mock.prototype.push = jest.fn();
        RelativeNavigationPlugin.extends(mock);
        const router = new mock();

        let current = {
            name: "mail-home",
            params: { folder: "INBOX" }
        };
        let location = router.relative("mail-root", current);
        expect(location.name).toEqual("mail-root");
        expect(location.params).toEqual({ folder: "INBOX" });
    });

    test("Only keep hash and qyery if we navigate inside the same route", () => {
        const mock = function () {};
        mock.prototype.push = jest.fn();
        RelativeNavigationPlugin.extends(mock);
        const router = new mock();

        let current = {
            name: "mail-thread",
            params: { folder: "INBOX", thread: "3" },
            hash: "#message-4",
            query: { collapse: true }
        };
        let location = router.relative("mail-root", current);
        expect(location.hash).toEqual("");
        expect(location.query).toEqual({});
        location = router.relative({ params: { mode: "reply" } }, current);
        expect(location.hash).toEqual("#message-4");
        expect(location.query).toEqual({ collapse: true });
    });

    test("Relative use currentRoute if a from is not specify", () => {
        const mock = function () {};
        mock.prototype.push = jest.fn();
        mock.prototype.currentRoute = {
            params: { old: "is old" }
        };
        RelativeNavigationPlugin.extends(mock);
        const router = new mock();
        let location = router.relative({ params: { neo: "is new" } });
        expect(location.params).toEqual({ old: "is old", neo: "is new" });
    });
    test("Navigate push relative location", () => {
        const mock = function () {};
        mock.prototype.push = jest.fn();
        RelativeNavigationPlugin.extends(mock);
        mock.prototype.relative = jest.fn().mockReturnValue("location");
        const router = new mock();
        router.navigate("to", "from");
        expect(router.relative).toHaveBeenCalledWith("to", "from");
        expect(router.push).toHaveBeenCalledWith("location");
    });
    test("Relative location does not contains extra parameters", () => {
        const mock = function () {};
        mock.prototype.push = jest.fn();
        RelativeNavigationPlugin.extends(mock);
        const router = new mock();

        let current = {
            name: "mail:home",
            meta: {},
            path: "/",
            hash: "",
            query: {},
            params: {},
            fullPath: "/mail/",
            matched: []
        };
        let location = router.relative("mail-root", current);
        expect(Object.keys(location)).toEqual(["name", "params", "hash", "query"]);
    });
});
