import {
    ACTIONS,
    CRITERIA_TARGETS,
    CRITERIA_MATCHERS,
    MATCH_ALL,
    read,
    write
} from "../components/preferences/fields/customs/FilterRules/filterRules.js";

describe("filters", () => {
    test("read", () => {
        const criteriaString = `FROM:IS: Toto TO:IS: Tata SUBJECT:CONTAINS: Toto&Tata 
        BODY:DOESNOTCONTAIN: un gros mot X-My-Header:MATCHES: head-bang`;
        const rawFilter = {
            active: true,
            criteria: criteriaString,
            delete: false,
            deliver: "INBOX",
            discard: false,
            forward: { enabled: true, localCopy: false, emails: ["toto.test@bluemind.net"] },
            name: "N/A",
            read: true,
            star: false,
            terminal: true
        };
        const filter = read(rawFilter);
        expect(filter).toMatchSnapshot();
    });

    test("read - no criteria", () => {
        const rawFilter = {
            active: true,
            criteria: MATCH_ALL,
            delete: false,
            deliver: "INBOX",
            discard: false,
            forward: { enabled: false, localCopy: true, emails: ["toto.test@bluemind.net"] },
            name: "N/A",
            read: true,
            star: false,
            terminal: true
        };
        const filter = read(rawFilter);
        expect(filter).toMatchSnapshot();
    });

    test("write", () => {
        const filter = {
            actions: [
                { type: ACTIONS.DELETE.type, value: true },
                {
                    type: ACTIONS.FORWARD.type,
                    value: { localCopy: false, emails: ["toto.test@bluemind.net"] }
                }
            ],
            active: true,
            criteria: [
                {
                    target: { type: CRITERIA_TARGETS.BODY.type, name: "BODY" },
                    matcher: CRITERIA_MATCHERS.DOESNOTCONTAIN,
                    value: "un gros mot"
                },
                {
                    target: { type: CRITERIA_TARGETS.TO.type, name: "TO" },
                    matcher: CRITERIA_MATCHERS.IS,
                    value: "Tata"
                },
                {
                    target: { type: CRITERIA_TARGETS.HEADER.type, name: "X-Machin-Chouette" },
                    matcher: CRITERIA_MATCHERS.IS,
                    value: "Bidule"
                }
            ],
            exceptions: [],
            name: "MyFilter",
            terminal: true
        };
        const rawFilter = write(filter);
        expect(rawFilter).toMatchSnapshot();
    });

    test("write - no criteria", () => {
        const filter = {
            actions: [
                { type: ACTIONS.DELETE.type, value: true },
                {
                    type: ACTIONS.FORWARD.type,
                    value: { localCopy: false, emails: ["toto.test@bluemind.net"] }
                }
            ],
            active: true,
            criteria: [],
            exceptions: [],
            name: "MyFilter",
            terminal: true
        };
        const rawFilter = write(filter);
        expect(rawFilter).toMatchSnapshot();
    });
});
