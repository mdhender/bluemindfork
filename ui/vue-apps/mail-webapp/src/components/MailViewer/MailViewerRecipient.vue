<template>
    <div class="mail-viewer-recipient d-flex mb-1">
        <span class="text-neutral"><slot />&nbsp;</span>
        <div v-overflown-elements class="d-flex overflow-hidden text-nowrap flex-fill" @overflown="hideContacts">
            <div
                v-for="(recipient, index) in recipients"
                :key="recipient.address + index"
                class="d-inline-flex viewer-recipient-item"
            >
                <bm-contact :contact="recipient" class="overflow-hidden" no-avatar transparent bold />
                <template v-if="index != recipients.length - hiddenContactCount - 1">,&nbsp;</template>
            </div>
        </div>
        <bm-more-items-badge
            ref="more-items-badge"
            class="pl-1"
            :value="hiddenContactCount"
            @click="$emit('show-more')"
        />
    </div>
</template>

<script>
import { BmContact, BmMoreItemsBadge, OverflownElements } from "@bluemind/styleguide";

export default {
    name: "MailViewerRecipient",
    components: { BmContact, BmMoreItemsBadge },
    directives: { OverflownElements },
    props: {
        recipients: {
            type: Array,
            required: true
        }
    },
    data() {
        return { hiddenContactCount: 0 };
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
