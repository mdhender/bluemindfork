<template>
    <chain-of-responsibility :is-responsible="isDSN">
        <div class="delivery-status-notice-top-frame d-flex flex-column p-5">
            <top-frame-skeleton v-if="success === undefined" />
            <template v-else>
                <div class="main d-flex flex-row align-items-center">
                    <bm-responsive-illustration
                        over-background
                        :value="success ? 'delivered-true' : 'delivered-false'"
                        class="mr-5"
                    />
                    <div class="spacer d-none d-lg-block ml-5"></div>
                    <div class="details flex-fill">
                        <i18n :path="summaryI18nPath">
                            <template v-if="originalMessage" #subject>
                                <template v-if="originalMessage.remoteRef">
                                    <router-link :to="link">{{
                                        originalMessage.subject || $t("mail.viewer.no.subject")
                                    }}</router-link>
                                </template>
                                <template v-else>{{ originalMessage.subject }}</template>
                            </template>
                            <template #recipient>
                                <span
                                    v-for="(recipient, index) in recipients"
                                    :key="index"
                                    class="font-weight-bold text-break-all"
                                    >{{
                                        recipients.length > 1 && index !== recipients.length - 1
                                            ? `${recipient}, `
                                            : recipient
                                    }}</span
                                >
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
                            <span v-if="success">{{
                                $t("mail.topframe.dsn.delivery_date", { dates: deliveryDates.join(", ") })
                            }}</span>
                        </div>
                        <span v-if="!success" class="d-none d-lg-block text-neutral">
                            {{ $t("mail.topframe.dsn.failed.notice") }}
                        </span>
                    </div>
                </div>
                <span v-if="!success" class="d-lg-none flex-fill text-neutral mt-4">
                    {{ $t("mail.topframe.dsn.failed.notice") }}
                </span>
            </template>
        </div>
    </chain-of-responsibility>
</template>

<script>
import { MimeType } from "@bluemind/email";
import { messageUtils } from "@bluemind/mail";
import { BmResponsiveIllustration } from "@bluemind/ui-components";
import { FETCH_PART_DATA } from "~/actions";
import ChainOfResponsibility from "../../../ChainOfResponsibility";
import ReportTopFrameMixin from "./ReportTopFrameMixin";
import TopFrameSkeleton from "./TopFrameSkeleton";

export default {
    name: "DeliveryStatusNoticeTopFrame",
    components: { BmResponsiveIllustration, ChainOfResponsibility, TopFrameSkeleton },
    mixins: [ReportTopFrameMixin],
    props: {
        message: { type: Object, required: true },
        files: { type: Array, required: true }
    },
    data() {
        return {
            deliveryDates: undefined,
            firstReport: undefined,
            isDSN: false,
            originalMessage: undefined,
            recipients: undefined,
            success: undefined
        };
    },
    priority: 0,
    computed: {
        summaryI18nPath() {
            return this.success
                ? this.originalMessage
                    ? "mail.topframe.dsn.summary"
                    : "mail.topframe.dsn.summary.no_subject"
                : this.originalMessage
                ? "mail.topframe.dsn.failed.summary"
                : "mail.topframe.dsn.failed.summary.no_subject";
        }
    },
    watch: {
        "message.structure": {
            handler: async function (structure) {
                const reportParts = messageUtils.getReportsParts(structure);
                if (reportParts?.length) {
                    this.firstReport = reportParts[0];
                    this.isDSN = this.firstReport?.mime === MimeType.MESSAGE_DELIVERY_STATUS;
                    if (this.isDSN) {
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
                        this.recipients = Object.keys(report);
                        this.deliveryDates = Object.values(report)
                            .map(subReport => {
                                const dateStr = subReport.arrivalDate || subReport.lastAttemptDate || this.message.date;
                                return dateStr ? this.$d(new Date(dateStr), "short_date_time") : undefined;
                            })
                            .filter(Boolean);

                        this.success = report[Object.keys(report)[0]].success;
                        this.originalMessage = await this.findOriginalMessage();
                    }
                }
            },
            immediate: true
        }
    },
    methods: {
        async findOriginalMessage() {
            const file = this.files.find(
                f => f.mime === MimeType.MESSAGE_RFC822 || f.mime === MimeType.TEXT_RFC822_HEADERS
            );
            if (file) {
                const fetched = await fetch(file.url);
                const blob = await fetched.blob();
                const { body } = await messageUtils.EmlParser.parseEml(blob);
                return this.findMessage(body.messageId) || body;
            }
        }
    }
};
/** @see rfc3464 */
function parseReportData(reportData) {
    const regex = /Final-Recipient\s*:/gi;
    let previousIndex = -1;
    let match;
    const report = {};
    while ((match = regex.exec(reportData))) {
        const index = match.index;
        if (previousIndex > -1 && index > previousIndex) {
            const subReport = parseReportData_(reportData.substring(previousIndex, index));
            report[subReport.finalRecipient] = subReport;
        }
        previousIndex = index;
    }
    const subReport = parseReportData_(reportData.substring(previousIndex));
    report[subReport.finalRecipient] = subReport;
    return report;
}
function parseReportData_(reportData) {
    const finalRecipientMatches = reportData.match(/Final-Recipient\s*:\s*(?:rfc822;\s)?(.*)/i);
    const finalRecipient = finalRecipientMatches?.length > 1 ? finalRecipientMatches[1] : undefined;
    const arrivalDateMatches = reportData.match(/Arrival-Date\s*:\s*(.*)/i);
    const arrivalDate = arrivalDateMatches?.length > 1 ? arrivalDateMatches[1] : undefined;
    const lastAttemptDateMatches = reportData.match(/Last-Attempt-Date\s*:\s*(.*)/i);
    const lastAttemptDate = lastAttemptDateMatches?.length > 1 ? lastAttemptDateMatches[1] : undefined;
    const actionMatches = reportData.match(/Action\s*:\s*(.*)/i);
    const action = actionMatches?.length > 1 ? actionMatches[1] : undefined;
    const success = ["delivered", "expanded", "relayed"].includes(action);
    return { arrivalDate, lastAttemptDate, finalRecipient, success };
}
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.delivery-status-notice-top-frame {
    background-color: $neutral-bg-lo1;
    @include regular-medium;

    .details {
        display: flex;
        flex-direction: column;
        gap: $sp-4;
    }
}
</style>
