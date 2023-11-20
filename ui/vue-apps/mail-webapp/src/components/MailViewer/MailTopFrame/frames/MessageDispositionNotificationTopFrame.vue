<template>
    <chain-of-responsibility :is-responsible="isMDN">
        <div class="message-disposition-notification-top-frame d-flex flex-column p-5">
            <top-frame-skeleton v-if="loading" />
            <template v-else>
                <div class="main d-flex flex-row align-items-center">
                    <bm-responsive-illustration over-background value="read" class="mr-5" />
                    <div class="spacer desktop-only ml-5"></div>
                    <div class="details flex-fill">
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
                        <div>
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
                        <em class="d-block desktop-only disclaimer">
                            {{ $t("mail.topframe.mdn.notice") }}
                        </em>
                    </div>
                </div>
                <em class="d-block mobile-only flex-fill disclaimer mt-4">
                    {{ $t("mail.topframe.mdn.notice") }}
                </em>
            </template>
        </div>
    </chain-of-responsibility>
</template>

<script>
import { MimeType } from "@bluemind/email";
import { BmResponsiveIllustration } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import { FETCH_PART_DATA } from "~/actions";
import ChainOfResponsibility from "../../../ChainOfResponsibility";
import ReportTopFrameMixin from "./ReportTopFrameMixin";
import TopFrameSkeleton from "./TopFrameSkeleton";

export default {
    name: "MessageDispositionNotificationTopFrame",
    components: { BmResponsiveIllustration, ChainOfResponsibility, TopFrameSkeleton },
    mixins: [ReportTopFrameMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { firstReport: undefined, isMDN: false, originalMessage: undefined, loading: false };
    },
    priority: 0,
    watch: {
        "message.structure": {
            handler: async function (structure) {
                const reportParts = messageUtils.getReportsParts(structure);
                if (reportParts?.length) {
                    this.loading = true;
                    this.firstReport = reportParts[0];
                    this.isMDN = this.firstReport?.mime === MimeType.MESSAGE_DISPOSITION_NOTIFICATION;
                    if (this.isMDN) {
                        await this.$store.dispatch(`mail/${FETCH_PART_DATA}`, {
                            messageKey: this.message.key,
                            folderUid: this.message.folderRef.uid,
                            imapUid: this.message.remoteRef.imapUid,
                            parts: [reportParts[0]]
                        });
                        const reportData =
                            this.$store.state.mail.partsData.partsByMessageKey[this.message.key][
                                this.firstReport.address
                            ];
                        const report = parseReportData(reportData);
                        this.originalMessage = await this.findMessage(report.originalMessageId);
                    }
                    this.loading = false;
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
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.message-disposition-notification-top-frame {
    background-color: $neutral-bg-lo1;
    @include regular-medium;

    .details {
        display: flex;
        flex-direction: column;
        gap: $sp-4;
    }

    .disclaimer {
        color: $neutral-fg;
        @include caption-italic;
        padding: $sp-3 0;
    }
}
</style>
