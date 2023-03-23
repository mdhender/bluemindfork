<template>
    <chain-of-responsibility :is-responsible="isMDN">
        <div v-if="isMDN" class="message-disposition-notification-top-frame">
            <!-- TODO change UI -->
            Message Disposition Notification report:<br />
            &emsp;Reporting-UA: {{ report.reportingUA }} <br />
            &emsp;Original-Recipient: {{ report.originalRecipient }} <br />
            &emsp;Final-Recipient: {{ report.finalRecipient }} <br />
            &emsp;Original-Message-ID: {{ report.originalMessageId }} <br />
            &emsp;Disposition: {{ report.disposition }} <br />
        </div>
    </chain-of-responsibility>
</template>

<script>
import { MimeType } from "@bluemind/email";
import { FETCH_PART_DATA } from "~/actions";
import ChainOfResponsibility from "../ChainOfResponsibility";

export default {
    name: "MessageDispositionNotificationTopFrame",
    components: { ChainOfResponsibility },
    props: { message: { type: Object, required: true } },
    data() {
        return { firstReport: undefined };
    },
    priority: 0,
    computed: {
        isMDN() {
            return this.firstReport?.mime === MimeType.MESSAGE_DISPOSITION_NOTIFICATION;
        },
        report() {
            let report;
            if (this.firstReport) {
                const reportData = this.$store.state.mail.partsData.partsByMessageKey[this.message.key][
                    this.firstReport.address
                ];
                report = reportData ? this.parseReportData(reportData) : {};
            }
            return report;
        }
    },
    watch: {
        "message.reports": {
            handler: function (reportParts) {
                if (reportParts?.length) {
                    this.firstReport = reportParts[0];
                    this.$store.dispatch(`mail/${FETCH_PART_DATA}`, {
                        messageKey: this.message.key,
                        folderUid: this.message.folderRef.uid,
                        imapUid: this.message.remoteRef.imapUid,
                        parts: [this.firstReport]
                    });
                }
            },
            immediate: true
        }
    },
    methods: {
        parseReportData(reportData) {
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
        }
    }
};
</script>
