<template>
    <bm-container class="mail-message-content bg-surface px-5 d-flex flex-column py-2">
        <bm-row>
            <bm-col cols="12">
                <mail-message-content-toolbar />
            </bm-col>
        </bm-row>
        <bm-row>
            <bm-col cols="12">
                <h2 class="text-secondary font-weight-normal">{{ subject }}</h2>
            </bm-col>
        </bm-row>
        <bm-row class="d-flex">
            <bm-col
                cols="8"
                class="d-flex"
            >
                <mail-message-content-from
                    :dn="message.from.dn"
                    :address="message.from.address"
                />
            </bm-col>
            <bm-col
                cols="4"
                class="align-self-center text-right"
            >
                {{ date }} {{ $t("mail.content.date.at") }} {{ hour }}
            </bm-col>
        </bm-row>
        <bm-row>
            <bm-col cols="12">
                <hr class="my-2">
            </bm-col>
        </bm-row>
        <bm-row>
            <bm-col cols="12">
                <mail-message-content-recipient
                    v-if="to"
                    :recipients="to"
                >
                    {{ $t("mail.content.to") }}
                </mail-message-content-recipient>
            </bm-col>
        </bm-row>
        <bm-row class="pb-2">
            <bm-col cols="12">
                <mail-message-content-recipient
                    v-if="cc"
                    :recipients="cc"
                >
                    {{ $t("mail.content.copy") }}
                </mail-message-content-recipient>
            </bm-col>
        </bm-row>
        <bm-row v-if="hasAttachments">
            <bm-col cols="12">
                <hr class="bg-dark my-0">
                <mail-message-content-attachment
                    :has-attachment="hasAttachments"
                    :attachments="attachments"
                />
            </bm-col>
        </bm-row>
        <bm-row
            ref="scrollableContainerForMailMessageContentBody"
            class="pt-1 flex-fill"
        >
            <bm-col col>
                <mail-message-content-body :parts="displayableParts" />
            </bm-col>
        </bm-row>
    </bm-container>
</template>

<script>
import { DateTimeFormat } from "@bluemind/i18n";
import { mapGetters } from "vuex";
import { MimeType } from "@bluemind/email";
import { BmCol, BmContainer, BmRow } from "@bluemind/styleguide";
import CommonL10N from "@bluemind/l10n";
import MailMessageContentAttachment from "./MailMessageContentAttachment";
import MailMessageContentBody from "./MailMessageContentBody";
import MailMessageContentFrom from "./MailMessageContentFrom";
import MailMessageContentRecipient from "./MailMessageContentRecipient";
import MailMessageContentToolbar from "./MailMessageContentToolbar";

const displayableMimeTypes = [MimeType.IMAGE];
const displayableMimeSubTypes = [MimeType.TEXT_PLAIN, MimeType.TEXT_HTML];

export default {
    name: "MailMessageContent",
    components: {
        BmCol,
        BmContainer,
        BmRow,
        MailMessageContentAttachment,
        MailMessageContentBody,
        MailMessageContentFrom,
        MailMessageContentRecipient,
        MailMessageContentToolbar
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        parts: {
            type: Array,
            required: true
        }
    },
    i18n: { messages: CommonL10N },
    data() {
        return {
            displayableParts: [],
            attachments: []
        };
    },
    computed: {
        ...mapGetters("backend.mail/folders", { folder: "currentFolder" }),
        ...mapGetters("backend.mail/items", { realAttachments: "attachments" }),
        to() {
            if (this.message.to.length > 0) {
                return this.message.to.map(dest => (dest.dn ? dest.dn : dest.address));
            }
            return "";
        },
        cc() {
            if (this.message.cc.length > 0) {
                return this.message.cc.map(dest => (dest.dn ? dest.dn : dest.address));
            }
            return "";
        },
        subject() {
            return this.message.subject || "(No subject)"; // FIXME i18n
        },
        date() {
            return DateTimeFormat.formatDayName(this.message.date) + " " + DateTimeFormat.formatDate(this.message.date);
        },
        hour() {
            return DateTimeFormat.formatHour(this.message.date);
        }
    },
    watch: {
        parts: {
            handler: function() {
                this.filterParts();
                this.resetScroll();
            },
            immediate: true
        }
    },
    created() {
        this.markAsSeen();
    },
    methods: {
        /** Filter displayable parts. Others will be considered as attachments. */
        filterParts() {
            let displayableParts = [];
            this.displayableParts.splice(0, this.displayableParts.length);
            let attachments = [];
            this.attachments.splice(0, this.displayableParts.length);
            if (this.parts) {
                this.parts.forEach(part => {
                    // check MIME type (like 'image')
                    let isDisplayable =
                        displayableMimeTypes.find(displayableMimeType =>
                            MimeType.typeEquals(displayableMimeType, part.mime)
                        ) !== undefined;
                    if (!isDisplayable) {
                        // check MIME sub-type (like 'image/png')
                        isDisplayable =
                            displayableMimeSubTypes.find(displayableMimeSubType =>
                                MimeType.equals(displayableMimeSubType, part.mime)
                            ) !== undefined;
                    }
                    if (isDisplayable) {
                        displayableParts.push(part);
                    } else {
                        // FIXME should pass the whole part or at least localize the default name
                        const attachmentName = part.fileName ? part.fileName : "Untitled " + part.mime;
                        let size = new String(part.size / 1000);
                        size = size.substring(0, size.indexOf("."));
                        if (size == 0) {
                            size = part.size + "o";
                        } else {
                            size = size + "Ko";
                        }
                        attachments.push(attachmentName + " " + size);
                    }
                });
            }
            this.displayableParts = [];
            this.displayableParts.push(...displayableParts);
            this.attachments = [];
            this.attachments.push(...this.realAttachments, ...attachments);
        },
        resetScroll() {
            this.$nextTick(() => {
                this.$refs.scrollableContainerForMailMessageContentBody.scrollTop = 0;
                this.$refs.scrollableContainerForMailMessageContentBody.scrollLeft = 0;
            });
        },
        hasAttachments() {
            return this.attachments.length > 0;
        },
        saveAttachments() {
            // not implemented yet
        },
        onLeave() {
            if (this.message.states.includes("not-seen")) {
                this.markAsSeen(this.message, this.folder);
            }
        },
        markAsSeen() {
            return this.$store.dispatch("backend.mail/items/updateSeen", {
                folder: this.folder,
                uid: this.message.uid,
                isSeen: true
            });
        }
    },
    beforeRouteUpdate(to, from, next) {
        if (to.params.mail != from.params.mail) {
            this.markAsSeen();
        }
        next();
    }
};
</script>
