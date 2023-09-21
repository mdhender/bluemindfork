export const REPLY_ACTIONS = {
    ACCEPTED: "Accepted",
    TENTATIVE: "Tentative",
    DECLINED: "Declined"
};

export const STATUS_KEY_FOR_OCCURRENCE = {
    [REPLY_ACTIONS.ACCEPTED]: "mail.viewer.invitation.reply.status.occurrence.accepted",
    [REPLY_ACTIONS.TENTATIVE]: "mail.viewer.invitation.reply.status.occurrence.tentative",
    [REPLY_ACTIONS.DECLINED]: "mail.viewer.invitation.reply.status.occurrence.declined"
};

export const STATUS_KEY_FOR_EVENT = {
    [REPLY_ACTIONS.ACCEPTED]: "mail.viewer.invitation.reply.status.event.accepted",
    [REPLY_ACTIONS.TENTATIVE]: "mail.viewer.invitation.reply.status.event.tentative",
    [REPLY_ACTIONS.DECLINED]: "mail.viewer.invitation.reply.status.event.declined"
};
