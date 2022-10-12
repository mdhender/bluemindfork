import {
    ACTIONS,
    CRITERIA_TARGETS,
    CRITERIA_MATCHERS,
    read,
    write
} from "../components/preferences/fields/customs/FilterRules/filterRules.js";

describe("filters", () => {
    test("read", () => {
        const rawFilter = {
            client: "bluemind",
            trigger: "IN",
            active: true,
            conditions: [
                {
                    operator: "AND",
                    negate: false,
                    filter: { fields: ["from.email"], operator: "EQUALS", values: ["Toto"] },
                    conditions: []
                },
                {
                    operator: "AND",
                    negate: false,
                    filter: { fields: ["to.email"], operator: "EQUALS", values: ["Tata"] },
                    conditions: []
                },
                {
                    operator: "AND",
                    negate: false,
                    filter: { fields: ["subject"], operator: "CONTAINS", values: ["Toto&Tata"] },
                    conditions: []
                },
                {
                    operator: "AND",
                    negate: true,
                    filter: { fields: ["part.content"], operator: "CONTAINS", values: ["un gros mot"] },
                    conditions: []
                },
                {
                    operator: "AND",
                    negate: false,
                    filter: { fields: ["headers.X-My-Header"], operator: "MATCHES", values: ["head-bang"] },
                    conditions: []
                }

            ],
            actions: [
                {
                    name: "MOVE",
                    folder: "INBOX"
                },
                {
                    name: "REDIRECT",
                    emails: "toto.test@bluemind.net",
                    keepCopy: false
                },
                {
                    name: "MARK_AS_READ"
                }
            ],
            name: "N/A",
            stop: true
        };
        const filter = read([rawFilter]);
        expect(filter).toMatchSnapshot();
    });

    test("read - no criteria", () => {
        const rawFilter = {
            client: "bluemind",
            trigger: "IN",
            active: true,
            conditions: [],
            actions: [
                {
                    name: "MOVE",
                    folder: "INBOX"
                },
                {
                    name: "REDIRECT",
                    emails: "toto.test@bluemind.net",
                    keepCopy: false
                },
                {
                    name: "MARK_AS_READ"
                }
            ],
            name: "N/A",
            stop: true
        };
        const filter = read([rawFilter]);
        expect(filter).toMatchSnapshot();
    });

    test("write", () => {
        const filter = {
            manageable: true,
            actions: [
                { name: ACTIONS.DELETE.name },
                {
                    name: ACTIONS.FORWARD.name,
                    keepCopy: false,
                    emails: ["toto.test@bluemind.net"]
                }
            ],
            active: true,
            criteria: [
                {
                    target: { type: CRITERIA_TARGETS.TO, name: "" },
                    matcher: CRITERIA_MATCHERS.EQUALS,
                    value: "Tata",
                    exception: false
                },
                {
                    target: { type: CRITERIA_TARGETS.HEADER, name: "X-Machin-Chouette" },
                    matcher: CRITERIA_MATCHERS.EQUALS,
                    value: "Bidule",
                    exception: false
                }
            ],
            exceptions: [
                {
                    target: { type: CRITERIA_TARGETS.BODY, name: "" },
                    matcher: CRITERIA_MATCHERS.CONTAINS,
                    value: "un gros mot",
                    exception: true
                }
            ],
            name: "MyFilter",
            terminal: true
        };
        const rawFilter = write(filter);
        expect(rawFilter).toMatchSnapshot();
    });

    test("write - no criteria", () => {
        const filter = {
            manageable: true,
            actions: [
                { name: ACTIONS.DELETE.name },
                {
                    name: ACTIONS.FORWARD.name,
                    keepCopy: false,
                    emails: ["toto.test@bluemind.net"]
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
