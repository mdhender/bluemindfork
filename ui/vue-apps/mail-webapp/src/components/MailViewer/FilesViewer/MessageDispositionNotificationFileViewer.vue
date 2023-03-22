<template>
    <div class="message-disposition-notification-file-viewer">
        Message Disposition Notification report:<br />
        &emsp;Reporting-UA: {{ report.reportingUA }} <br />
        &emsp;Original-Recipient: {{ report.originalRecipient }} <br />
        &emsp;Final-Recipient: {{ report.finalRecipient }} <br />
        &emsp;Original-Message-ID: {{ report.originalMessageId }} <br />
        &emsp;Disposition: {{ report.disposition }} <br />
    </div>
</template>

<script>
import { FETCH_PART_DATA } from "~/actions";
import FileViewerMixin from "./FileViewerMixin";

export default {
    name: "MessageDispositionNotificationFileViewer",
    mixins: [FileViewerMixin],
    computed: {
        report() {
            const reportData = this.$store.state.mail.partsData.partsByMessageKey[this.message.key][this.file.address];
            return reportData ? this.parseReportData(reportData) : {};
        }
    },
    watch: {
        file: {
            handler: function (reportPart) {
                if (reportPart) {
                    this.$store.dispatch(`mail/${FETCH_PART_DATA}`, {
                        messageKey: this.message.key,
                        folderUid: this.message.folderRef.uid,
                        imapUid: this.message.remoteRef.imapUid,
                        parts: [reportPart]
                    });
                }
            },
            immediate: true
        }
    },
    methods: {
        parseReportData(reportData) {
            reportData = this.sanitize(reportData);
            const reportingUAMatches = reportData.match(/Reporting-UA\s*:\s*(.*)/i);
            const reportingUA = reportingUAMatches?.length > 1 ? reportingUAMatches[1] : undefined;
            const originalRecipientMatches = reportData.match(/Original-Recipient\s*:\s*(.*)/i);
            const originalRecipient = originalRecipientMatches?.length > 1 ? originalRecipientMatches[1] : undefined;
            const finalRecipientMatches = reportData.match(/Final-Recipient\s*:\s*(.*)/i);
            const finalRecipient = finalRecipientMatches?.length > 1 ? finalRecipientMatches[1] : undefined;
            const originalMessageIdMatches = reportData.match(/Original-Message-ID\s*:\s*(.*)/i);
            const originalMessageId = originalMessageIdMatches?.length > 1 ? originalMessageIdMatches[1] : undefined;
            const dispositionMatches = reportData.match(/Disposition\s*:\s*(.*)/i);
            const disposition = dispositionMatches?.length > 1 ? dispositionMatches[1] : undefined;
            return { reportingUA, originalRecipient, finalRecipient, originalMessageId, disposition };
        },
        sanitize(reportData) {
            return reportData.replaceAll("\\n", "\n");
        }
    }
};
</script>
