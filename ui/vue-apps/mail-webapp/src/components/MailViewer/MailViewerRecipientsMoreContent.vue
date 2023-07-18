<template>
    <div class="mail-viewer-recipients-more-content" tabindex="0" @keyup.esc="$emit('close')">
        <div class="header">
            <div class="subject-and-date">
                <div class="d-flex">
                    <div class="custom-col-left">{{ $t("common.subject") }}</div>
                    <div class="custom-col-right">{{ message.subject }}</div>
                </div>
                <div class="d-flex">
                    <div class="custom-col-left">{{ $t("common.date") }}</div>
                    <div class="custom-col-right">{{ new Date(message.date).toLocaleString() }}</div>
                </div>
            </div>
            <bm-button-close v-if="!hideClose" size="lg" @click="$emit('close')" />
        </div>
        <div class="body scroller-y">
            <div class="d-flex">
                <div class="custom-col-left">{{ $t("common.from") }}</div>
                <div class="custom-col-right">
                    <mail-contact-card-slots
                        :component="Contact"
                        :contact="message.from"
                        no-avatar
                        transparent
                        bold-dn
                        show-address
                        :text-truncate="false"
                        enable-card
                    />
                </div>
            </div>
            <div class="d-flex">
                <div class="custom-col-left">{{ $t("common.to") }}</div>
                <div class="custom-col-right">
                    <mail-contact-card-slots
                        v-for="(contact, index) in message.to"
                        :key="`${contact.address}#${index}`"
                        :component="Contact"
                        :contact="contact"
                        no-avatar
                        transparent
                        bold-dn
                        show-address
                        :text-truncate="false"
                        enable-card
                    />
                </div>
            </div>
            <div v-if="message.cc && message.cc.length" class="d-flex">
                <div class="custom-col-left">{{ $t("common.cc") }}</div>
                <div class="custom-col-right">
                    <mail-contact-card-slots
                        v-for="(contact, index) in message.cc"
                        :key="`${contact.address}#${index}`"
                        :component="Contact"
                        :contact="contact"
                        no-avatar
                        transparent
                        bold-dn
                        show-address
                        :text-truncate="false"
                        enable-card
                    />
                </div>
            </div>
            <div v-if="message.bcc && message.bcc.length" class="d-flex">
                <div class="custom-col-left">{{ $t("common.bcc") }}</div>
                <div class="custom-col-right">
                    <mail-contact-card-slots
                        v-for="(contact, index) in message.bcc"
                        :key="`${contact.address}#${index}`"
                        :component="Contact"
                        :contact="contact"
                        no-avatar
                        transparent
                        bold-dn
                        show-address
                        :text-truncate="false"
                        enable-card
                    />
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import { Contact } from "@bluemind/business-components";
import { BmButtonClose } from "@bluemind/ui-components";
import MailContactCardSlots from "../MailContactCardSlots";

export default {
    name: "MailViewerRecipientsMoreContent",
    components: { BmButtonClose, MailContactCardSlots },
    props: {
        message: {
            type: Object,
            required: true
        },
        hideClose: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return { Contact };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-viewer-recipients-more-content {
    display: flex;
    flex-direction: column;
    align-items: stretch;

    .header {
        display: flex;
        gap: $sp-5;
        padding-top: $sp-6;
        @include from-lg {
            padding-top: $sp-5;
        }
        background-color: $neutral-bg-lo1;

        .subject-and-date {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: $sp-5;
            @include from-lg {
                padding-top: base-px-to-rem(7);
            }
        }

        border-bottom: 1px solid $neutral-fg-lo3;

        padding-right: $sp-3;
        padding-bottom: calc(#{$sp-6} - 1px);
        @include from-lg {
            padding-right: base-px-to-rem(14);
            padding-bottom: calc(#{$sp-5 + $sp-3} - 1px);
        }
    }

    .body {
        min-height: 0;
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: $sp-5;
        padding-top: $sp-6;
        @include from-lg {
            padding-top: $sp-5 + $sp-3;
        }
        padding-bottom: $sp-6;
    }

    .custom-col-left {
        flex: none;
        width: base-px-to-rem(80);
        @include from-lg {
            width: base-px-to-rem(90);
        }
        color: $neutral-fg-hi1;
        @include regular;
        text-align: right;
        padding-right: $sp-6;
    }
    .custom-col-right {
        min-width: 0;
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: $sp-3;
        padding-right: $sp-6;
        overflow-wrap: break-word;
        word-break: break-all;
        padding-right: $sp-3;
        @include from-lg {
            padding-right: $sp-7;
        }
        @include regular;
    }
}
</style>
