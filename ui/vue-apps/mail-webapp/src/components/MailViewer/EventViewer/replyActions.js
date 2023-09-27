export const REPLY_ACTIONS = {
    ACCEPTED: "Accepted",
    TENTATIVE: "Tentative",
    DECLINED: "Declined",
    NEEDS_ACTION: "NeedsAction"
};

export const STATUS_KEY_FOR_OCCURRENCE = {
    [REPLY_ACTIONS.ACCEPTED]: "mail.viewer.invitation.reply.status.occurrence.accepted",
    [REPLY_ACTIONS.TENTATIVE]: "mail.viewer.invitation.reply.status.occurrence.tentative",
    [REPLY_ACTIONS.DECLINED]: "mail.viewer.invitation.reply.status.occurrence.declined",
    [REPLY_ACTIONS.NEEDS_ACTION]: "mail.viewer.invitation.reply.status.occurrence.needs_action"
};

export const STATUS_KEY_FOR_EVENT = {
    [REPLY_ACTIONS.ACCEPTED]: "mail.viewer.invitation.reply.status.event.accepted",
    [REPLY_ACTIONS.TENTATIVE]: "mail.viewer.invitation.reply.status.event.tentative",
    [REPLY_ACTIONS.DECLINED]: "mail.viewer.invitation.reply.status.event.declined",
    [REPLY_ACTIONS.NEEDS_ACTION]: "mail.viewer.invitation.reply.status.event.needs_action"
};
