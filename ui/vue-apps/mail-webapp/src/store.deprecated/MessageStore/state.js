export default function () {
    return {
        id: undefined,
        key: undefined,
        parts: { attachments: [], inlines: [] },
        saveDate: null,
        status: null,
        attachmentStatuses: {},
        attachmentProgresses: {},
        to: [],
        cc: [],
        bcc: [],
        subject: "",
        content: "",
        isReplyExpanded: false,
        type: undefined,
        previousMessage: null,
        headers: []
    };
}
