<template>
    <div class="mail-viewer-recipient medium d-flex position-relative align-items-center">
        <span class="text-neutral"><slot />&nbsp;</span>
        <div v-overflown-elements class="d-flex overflow-hidden text-nowrap flex-fill" @overflown="hideContacts">
            <div
                v-for="(recipient, index) in recipients"
                :key="recipient.address + index"
                class="d-inline-flex viewer-recipient-item align-items-center"
            >
                <mail-contact-card-slots
                    :component="Contact"
                    :contact="recipient"
                    class="overflow-hidden"
                    no-avatar
                    transparent
                    bold
                    enable-card
                />
                <template v-if="index != recipients.length - hiddenContactCount - 1">,&nbsp;</template>
            </div>
        </div>
        <bm-more-items-badge
            ref="more-items-badge"
            class="pl-4"
            :count="hiddenContactCount"
            @click="$emit('show-more')"
        />
    </div>
</template>

<script>
import { BmMoreItemsBadge, OverflownElements } from "@bluemind/ui-components";
import { Contact } from "@bluemind/business-components";
import MailContactCardSlots from "../MailContactCardSlots";

export default {
    name: "MailViewerRecipient",
    components: { BmMoreItemsBadge, MailContactCardSlots },
    directives: { OverflownElements },
    props: {
        recipients: {
            type: Array,
            required: true
        }
    },
    data() {
        return { hiddenContactCount: 0, Contact };
    },
    methods: {
        hideContacts(overflownEvent) {
            const badge = this.$refs["more-items-badge"];
            if (badge) {
                this.hiddenContactCount = badge.hideOverflownElements({
                    overflownEvent,
                    elementClass: "viewer-recipient-item"
                });
            }
        }
    }
};
</script>

<style lang="scss">
.mail-viewer-recipient {
    .viewer-recipient-item {
        max-width: 90%;
    }
}
</style>
