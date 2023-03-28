<template>
    <chain-of-responsibility :is-responsible="isMDN">
        <div class="message-disposition-notification-top-frame d-flex flex-column p-5">
            <div class="d-flex flex-row align-items-center">
                <img :src="mdnImage" class="mr-5" />
                <div class="flex-fill">
                    <p class="mb-3">
                        <i18n
                            :path="
                                originalMessage ? 'mail.topframe.mdn.summary' : 'mail.topframe.mdn.summary.no_subject'
                            "
                        >
                            <template v-if="originalMessage" #subject>
                                <router-link :to="link">{{
                                    originalMessage.subject || $t("mail.viewer.no.subject")
                                }}</router-link>
                            </template>
                            <template #sender>
                                <span v-if="message.from.dn" class="font-weight-bold">{{ message.from.dn }}</span>
                                <span v-if="message.from.dn && message.from.address">&nbsp;&lt;</span
                                ><span
                                    v-if="message.from.address"
                                    class="text-break-all"
                                    :class="{ 'font-weight-bold': !message.from.dn }"
                                    >{{ message.from.address }}</span
                                ><span v-if="message.from.dn && message.from.address">&gt;</span>
                            </template>
                        </i18n>
                    </p>
                    <div class="medium mb-3">
                        <template v-if="originalMessage">
                            <span>
                                {{
                                    $t("mail.topframe.report.send_date", {
                                        date: $d(originalMessage.date, "short_date_time")
                                    })
                                }}
                            </span>
                            <br />
                        </template>
                        <span>{{
                            $t("mail.topframe.mdn.opened_date", { date: $d(message.date, "short_date_time") })
                        }}</span>
                    </div>
                    <span class="d-none d-lg-block">
                        <em class="text-neutral caption-italic">{{ $t("mail.topframe.mdn.notice") }}</em>
                    </span>
                </div>
            </div>
            <span class="d-lg-none flex-fill">
                <em class="text-neutral caption-italic">{{ $t("mail.topframe.mdn.notice") }}</em>
            </span>
        </div>
    </chain-of-responsibility>
</template>

<script>
import { MimeType } from "@bluemind/email";
import { FETCH_PART_DATA } from "~/actions";
import ChainOfResponsibility from "../ChainOfResponsibility";
import mdnImage from "./mdn.png";
import ReportTopFrameMixin from "./ReportTopFrameMixin";

export default {
    name: "MessageDispositionNotificationTopFrame",
    components: { ChainOfResponsibility },
    mixins: [ReportTopFrameMixin],
    props: { message: { type: Object, default: undefined } },
    data() {
        return { firstReport: undefined, isMDN: false, mdnImage, originalMessage: undefined };
    },
    priority: 0,
    watch: {
        "message.reports": {
            handler: async function (reportParts) {
                if (reportParts?.length) {
                    this.firstReport = reportParts[0];
                    this.isMDN = this.firstReport?.mime === MimeType.MESSAGE_DISPOSITION_NOTIFICATION;
                    if (this.isMDN) {
                        await this.$store.dispatch(`mail/${FETCH_PART_DATA}`, {
                            messageKey: this.message.key,
                            folderUid: this.message.folderRef.uid,
                            imapUid: this.message.remoteRef.imapUid,
                            parts: [reportParts[0]]
                        });
                        const reportData = this.$store.state.mail.partsData.partsByMessageKey[this.message.key][
                            this.firstReport.address
                        ];
                        const report = parseReportData(reportData);
                        this.originalMessage = await this.findMessage(report.originalMessageId);
                    }
                }
            },
            immediate: true
        }
    }
};

/**
 * We use only the "Original-Message-ID" field.
 * @see rfc8098 for other fields.
 */
function parseReportData(reportData) {
    const originalMessageIdMatches = reportData.match(/Original-Message-ID\s*:\s*(.*)/i);
    const originalMessageId = originalMessageIdMatches?.length > 1 ? originalMessageIdMatches[1] : undefined;
    return { originalMessageId };
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/_type";
@import "~@bluemind/ui-components/src/css/variables";

.message-disposition-notification-top-frame {
    background-color: $neutral-bg-lo1;
    @extend %regular;
}
</style>
