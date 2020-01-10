/**
 * Flags for the display mode of MailMessageNew's recipients fields.
 *
 * @example
 * MailMessageNew.mode = (TO|CC|BCC) // means we would like to display all 3 fields
 * MailMessageNew.mode = TO // means we would like the TO field only
 */
export default {
    NONE: 0,
    TO: 1,
    CC: 2,
    BCC: 4
};