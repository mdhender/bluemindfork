export const RECORD_QUERY_FIELDS = {
    OWNER: "owner",
    IN: "in",
    UID: "uid",
    WITH: "with",
    HEADERS: "headers",
    SIZE: "size",
    HAS: "has",
    IS: "is"
};
export const QUERY_FIELDS = {
    SUBJECT: "subject",
    REFERENCES: "references",
    CONTENT: "content",
    FILENAME: "filename",
    FROM: "from",
    TO: "to",
    CC: "cc",
    DATE: "date"
};

const PATTERN_KEYWORDS = {
    ...RECORD_QUERY_FIELDS,
    ...QUERY_FIELDS
};
export default PATTERN_KEYWORDS;