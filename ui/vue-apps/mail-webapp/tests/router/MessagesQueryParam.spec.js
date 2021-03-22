import MessageQueryParam from "../../src/router/MessageQueryParam";

describe("MessageQueryParam", () => {
    test("Extract folder from a path", () => {
        let params = MessageQueryParam.parse("/.d/INBOX");
        expect(params.folder).toEqual("INBOX");
        let definedKeys = Object.keys(params).filter(key => !!params[key]);
        expect(definedKeys).toEqual(["folder"]);
        params = MessageQueryParam.parse("/.m/a.mailbox/.d/INBOX/.s/search%20pattern/.f/unread");
        expect(params.folder).toEqual("INBOX");
    });
    test("Extract mailbox from a path", () => {
        let params = MessageQueryParam.parse("/.m/MAILBOX");
        expect(params.mailbox).toEqual("MAILBOX");
        let definedKeys = Object.keys(params).filter(key => !!params[key]);
        expect(definedKeys).toEqual(["mailbox"]);
        params = MessageQueryParam.parse("/.m/a.mailbox/.d/INBOX/.s/search%20pattern/.f/unread");
        expect(params.mailbox).toEqual("a.mailbox");
    });
    test("Extract filter from a path", () => {
        let params = MessageQueryParam.parse("/.f/unread");
        expect(params.filter).toEqual("unread");
        let definedKeys = Object.keys(params).filter(key => !!params[key]);
        expect(definedKeys).toEqual(["filter"]);
        params = MessageQueryParam.parse("/.m/a.mailbox/.d/INBOX/.s/search%20pattern/.f/unread");
        expect(params.filter).toEqual("unread");
    });
    test("Extract search from a path", () => {
        let params = MessageQueryParam.parse("/.s/search pattern");
        expect(params.search).toEqual("search pattern");
        let definedKeys = Object.keys(params).filter(key => !!params[key]);
        expect(definedKeys).toEqual(["search"]);
        params = MessageQueryParam.parse("/.m/a.mailbox/.d/INBOX/.s/search%20pattern/.f/unread");
        expect(params.search).toEqual("search%20pattern");
    });
    test("Mailbox and folder support / and dot inside value", () => {
        let params = MessageQueryParam.parse("/.m/a.mailbox/.d/My/Folder/is.nice/.s/x/");
        expect(params.folder).toEqual("My/Folder/is.nice");
        expect(params.mailbox).toEqual("a.mailbox");
    });
    test("Search support every kind of chars inside name", () => {
        let params = MessageQueryParam.parse("/.s/Searching @ ? or ! is ... ÉURK%*$!");
        expect(params.search).toEqual("Searching @ ? or ! is ... ÉURK%*$!");
    });
    test("A path builded with MessageContext must be consistent", () => {
        let path = MessageQueryParam.build(undefined, {
            folder: "My/Folder",
            mailbox: "a.mailbox/Send",
            search: "search pattern",
            filter: "unread"
        });
        let params = MessageQueryParam.parse(path);
        expect(params.folder).toEqual("My/Folder");
        expect(params.mailbox).toEqual("a.mailbox/Send");
        expect(params.search).toEqual("search pattern");
        expect(params.filter).toEqual("unread");
    });
    test("A path builded with MessageContext keep previous parameters", () => {
        let old = "/.m/a.mailbox/.d/INBOX/.s/search pattern/.f/unread";
        let path = MessageQueryParam.build(old, {});
        let params = MessageQueryParam.parse(path);
        expect(params.folder).toEqual("INBOX");
        expect(params.mailbox).toEqual("a.mailbox");
        expect(params.search).toEqual("search pattern");
        expect(params.filter).toEqual("unread");
    });
    test("When building a path, new parameters overwride old ones", () => {
        let old = "/.m/a.mailbox/.d/INBOX/.s/search pattern/.f/unread";
        let path = MessageQueryParam.build(old, { folder: "1", mailbox: "2", search: "3", filter: "4" });
        let params = MessageQueryParam.parse(path);
        expect(params.folder).toEqual("1");
        expect(params.mailbox).toEqual("2");
        expect(params.search).toEqual("3");
        expect(params.filter).toEqual("4");
        path = MessageQueryParam.build(old, { mailbox: "2", search: "3", filter: "4" });
        params = MessageQueryParam.parse(path);
        expect(params.folder).toEqual("INBOX");
        expect(params.mailbox).toEqual("2");
        expect(params.search).toEqual("3");
        expect(params.filter).toEqual("4");
    });
    test("When building a path, new parameters with empty value erase old values", () => {
        let old = "/.m/a.mailbox/.d/INBOX/.s/search pattern/.f/unread";
        let path = MessageQueryParam.build(old, { folder: undefined, mailbox: null, search: "", filter: false });
        expect(path).toEqual("");
        path = MessageQueryParam.build(old, {
            folder: undefined,
            mailbox: "my/mailbox",
            search: "",
            filter: false
        });
        let params = MessageQueryParam.parse(path);
        expect(params.mailbox).toEqual("my/mailbox");
        expect(params.folder).not.toBeDefined();
        expect(params.search).not.toBeDefined();
        expect(params.filter).not.toBeDefined();
    });
});
