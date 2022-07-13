<template>
    <div class="mail-viewer-recipients-more-content" tabindex="0" @keyup.esc="$emit('close')">
        <div v-if="!hideClose" class="d-flex position-sticky top-0 pt-1">
            <div class="flex-fill">
                <bm-button-close class="align-self-end" @click="$emit('close')" />
            </div>
        </div>
        <div class="overflow-auto pb-2">
            <div class="d-flex">
                <div class="custom-col-left text-right pr-2">{{ $t("common.from") }}</div>
                <div class="custom-col-right">
                    <bm-contact :contact="message.from" no-avatar transparent bold show-address />
                </div>
            </div>
            <div class="d-flex">
                <div class="custom-col-left text-right pr-2">{{ $t("common.to") }}</div>
                <div class="custom-col-right d-flex flex-column">
                    <bm-contact
                        v-for="(contact, index) in message.to"
                        :key="`${contact.address}#${index}`"
                        :contact="contact"
                        no-avatar
                        transparent
                        bold
                        show-address
                        :text-truncate="false"
                    />
                </div>
            </div>
            <div v-if="message.cc && message.cc.length" class="d-flex">
                <div class="custom-col-left text-right pr-2">{{ $t("common.cc") }}</div>
                <div class="custom-col-right d-flex flex-column">
                    <bm-contact
                        v-for="(contact, index) in message.cc"
                        :key="`${contact.address}#${index}`"
                        :contact="contact"
                        no-avatar
                        transparent
                        bold
                        show-address
                        :text-truncate="false"
                    />
                </div>
            </div>
            <div v-if="message.bcc && message.bcc.length" class="d-flex">
                <div class="custom-col-left text-right pr-2">{{ $t("common.bcc") }}</div>
                <div class="custom-col-right d-flex flex-column">
                    <bm-contact
                        v-for="(contact, index) in message.bcc"
                        :key="`${contact.address}#${index}`"
                        :contact="contact"
                        no-avatar
                        transparent
                        bold
                        show-address
                        :text-truncate="false"
                    />
                </div>
            </div>
            <div class="d-flex">
                <div class="custom-col-left" />
                <div class="custom-col-right"><hr class="mb-1 mt-3" /></div>
            </div>
            <div class="d-flex">
                <div class="custom-col-left text-right pr-2">{{ $t("common.date") }}</div>
                <div class="fcustom-col-right font-weight-bold">{{ new Date(message.date).toLocaleString() }}</div>
            </div>
            <div class="d-flex">
                <div class="custom-col-left text-right pr-2">{{ $t("common.subject") }}</div>
                <div class="custom-col-right font-weight-bold">{{ message.subject }}</div>
            </div>
        </div>
    </div>
</template>

<script>
import { BmButtonClose, BmContact } from "@bluemind/styleguide";

export default {
    name: "MailViewerRecipientsMoreContent",
    components: { BmButtonClose, BmContact },
    props: {
        message: {
            type: Object,
            required: true
        },
        hideClose: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-viewer-recipients-more-content {
    $left-col-width: 4rem;
    .custom-col-left {
        width: $left-col-width;
        color: $neutral-fg;
    }
    .custom-col-right {
        width: calc(100% - $left-col-width);
    }
}
</style>
