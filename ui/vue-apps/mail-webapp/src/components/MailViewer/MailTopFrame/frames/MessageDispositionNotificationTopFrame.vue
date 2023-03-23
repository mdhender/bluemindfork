<template>
    <chain-of-responsibility :is-responsible="isMDN">
        <div v-if="isMDN" class="message-disposition-notification-top-frame d-flex flex-column p-5">
            <div class="d-flex flex-row align-items-center">
                <img :src="mdnImage" class="mr-5" />
                <div class="flex-fill">
                    <p class="mb-3">
                        <i18n
                            :path="
                                originalMessage ? 'mail.topframe.mdn.summary' : 'mail.topframe.mdn.summary.no_subject'
                            "
                            class="text-break"
                        >
                            <template v-if="originalMessage" #subject>
                                <router-link to="path/to/message">{{ originalMessage.subject }}</router-link>
                            </template>
                            <template #sender>
                                <span v-if="message.from.dn" class="font-weight-bold">{{ message.from.dn }}</span>
                                <span v-if="message.from.dn && message.from.address">&nbsp;&lt;</span
                                ><span v-if="message.from.address" :class="{ 'font-weight-bold': !message.from.dn }">{{
                                    message.from.address
                                }}</span
                                ><span v-if="message.from.dn && message.from.address">&gt;</span>
                            </template>
                        </i18n>
                    </p>
                    <div class="medium mb-3">
                        <template v-if="originalMessage">
                            <span>
                                {{ $t("mail.topframe.mdn.send_date", { date: originalMessage.date.toLocaleString() }) }}
                            </span>
                            <br />
                        </template>
                        <span>{{ $t("mail.topframe.mdn.opened_date", { date: message.date.toLocaleString() }) }}</span>
                    </div>
                    <span class="d-none d-lg-block">
                        <em class="text-neutral">{{ $t("mail.topframe.mdn.notice") }}</em>
                    </span>
                </div>
            </div>
            <span class="d-lg-none flex-fill">
                <em class="text-neutral">{{ $t("mail.topframe.mdn.notice") }}</em>
            </span>
        </div>
    </chain-of-responsibility>
</template>

<script>
import { MimeType } from "@bluemind/email";
import { FETCH_PART_DATA } from "~/actions";
import ChainOfResponsibility from "../ChainOfResponsibility";
import mdnImage from "./mdn.png";

export default {
    name: "MessageDispositionNotificationTopFrame",
    components: { ChainOfResponsibility },
    props: { message: { type: Object, required: true } },
    data() {
        return {
            mdnImage,
            firstReport: undefined,
            originalMessage: { subject: "Bla bla sujet", date: new Date("1970") }
        };
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

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.message-disposition-notification-top-frame {
    background-color: $neutral-bg-lo1;
}
</style>
