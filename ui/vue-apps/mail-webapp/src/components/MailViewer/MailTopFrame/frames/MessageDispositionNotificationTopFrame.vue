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
                                <router-link :to="link">{{ originalMessage.subject }}</router-link>
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
import { inject } from "@bluemind/inject";
import { conversationUtils } from "@bluemind/mail";
import { FETCH_PART_DATA } from "~/actions";
import { FolderAdaptor } from "~/store/folders/helpers/FolderAdaptor";
import ChainOfResponsibility from "../ChainOfResponsibility";
import mdnImage from "./mdn.png";

export default {
    name: "MessageDispositionNotificationTopFrame",
    components: { ChainOfResponsibility },
    props: { message: { type: Object, required: true } },
    data() {
        return { mdnImage, firstReport: undefined, originalMessage: undefined };
    },
    priority: 0,
    computed: {
        isMDN() {
            return this.firstReport?.mime === MimeType.MESSAGE_DISPOSITION_NOTIFICATION;
        },
        link() {
            const conversation = conversationUtils.createConversationStub(
                this.originalMessage.remoteRef.internalId,
                this.originalMessage.folderRef
            );
            const folder = this.$store.state.mail.folders[this.originalMessage.folderRef.key];
            return {
                name: "v:mail:conversation",
                params: {
                    conversation,
                    folder: folder?.path,
                    mailbox: this.$store.state.mail.mailboxes[folder?.mailboxRef.key]?.name
                }
            };
        }
    },
    watch: {
        "message.reports": {
            handler: async function (reportParts) {
                if (reportParts?.length) {
                    await this.$store.dispatch(`mail/${FETCH_PART_DATA}`, {
                        messageKey: this.message.key,
                        folderUid: this.message.folderRef.uid,
                        imapUid: this.message.remoteRef.imapUid,
                        parts: [reportParts[0]]
                    });
                    this.firstReport = reportParts[0];
                    const reportData = this.$store.state.mail.partsData.partsByMessageKey[this.message.key][
                        this.firstReport.address
                    ];
                    const report = this.parseReportData(reportData);
                    this.originalMessage = await this.findOriginalMessage(report.originalMessageId);
                }
            },
            immediate: true
        }
    },
    methods: {
        /**
         * We use only the "Original-Message-ID" field.
         * @see rfc8098 for other fields.
         */
        parseReportData(reportData) {
            const originalMessageIdMatches = reportData.match(/Original-Message-ID\s*:\s*(.*)/i);
            const originalMessageId = originalMessageIdMatches?.length > 1 ? originalMessageIdMatches[1] : undefined;
            return { originalMessageId };
        },
        async findOriginalMessage(originalMessageId) {
            const messages = this.$store.state.mail.conversations.messages;
            // search in store
            let message = Object.values(messages).find(value => value?.messageId === originalMessageId);
            if (!message) {
                // search on server
                const mboxUid = this.$store.state.mail.folders[this.message.folderRef.key].mailboxRef.uid;
                const searchResult = await inject("MailboxFoldersPersistence", mboxUid).searchItems({
                    query: { messageId: originalMessageId, maxResults: 1, scope: { folderScope: {} } }
                });
                const result = searchResult?.totalResults > 0 && searchResult.results[0];
                const fromOrToFn = fromOrTo => {
                    const isArray = Array.isArray(fromOrTo);
                    let asArray = isArray ? fromOrTo : [fromOrTo];
                    const normalized = asArray.map(({ displayName, address }) => ({ dn: displayName, address }));
                    return isArray ? normalized : normalized[0];
                };
                message = result
                    ? {
                          subject: result.subject,
                          date: new Date(result.date),
                          from: fromOrToFn(result.from),
                          to: fromOrToFn(result.to),
                          remoteRef: { internalId: result.itemId, imapUid: result.imapUid },
                          folderRef: FolderAdaptor.toRef(FolderAdaptor.extractFolderUid(result.containerUid))
                      }
                    : undefined;
            }
            return message;
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
