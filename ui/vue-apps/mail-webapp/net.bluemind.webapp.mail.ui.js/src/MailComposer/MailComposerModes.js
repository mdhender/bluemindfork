/**
 * Flags for the display mode of MailComposer's recipients fields.
 *
 * @example
 * MailComposer.mode = (TO|CC|BCC) // means we would like to display all 3 fields
 * MailComposer.mode = TO // means we would like the TO field only
 */
export default {
    NONE: 0,
    TO: 1,
    CC: 2,
    BCC: 4
};
