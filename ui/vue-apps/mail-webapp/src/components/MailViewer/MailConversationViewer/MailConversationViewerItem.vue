<template>
    <div class="mail-conversation-viewer-item">
        <div
            class="mail-conversation-viewer-item-body"
            :class="{ 'last-before-draft': isLastBeforeDraft }"
            tabindex="0"
            @click.prevent="expand"
            @keypress.enter="toggle"
        >
            <div v-if="index !== 0" class="d-flex conversation-viewer-row flex-nowrap">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" />
                <div class="flex-fill spacer" />
            </div>
            <div class="d-flex min-height conversation-viewer-row click-to-collapse-zone flex-nowrap" @click="collapse">
                <div class="avatar-wrapper vertical-line" :class="{ first: index === 0, last: index === maxIndex }">
                    <bm-contact :contact="message.from" no-text avatar-size="md" />
                </div>
                <slot name="head" />
            </div>
            <slot name="subhead" />
            <div
                v-if="isMessageExpanded && !message.composing"
                class="d-flex pr-5 conversation-viewer-row flex-nowrap click-to-collapse-zone"
                @click="collapse"
            >
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <mail-viewer-recipients :message="message" class="flex-fill mt-2 mb-4" @click.native.stop />
            </div>
            <div class="d-flex conversation-viewer-row flex-nowrap">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <slot name="content" />
            </div>
            <div class="d-flex conversation-viewer-row flex-nowrap" :class="{ 'sticky-bottom': stickyBottom }">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <slot name="bottom" />
            </div>
        </div>

        <div v-if="nextIsDraft || (isDraft && index !== maxIndex)" class="d-flex contrast">
            <div class="col spacer" />
        </div>
        <div
            v-else-if="(!nextIsHidden || index === maxIndex || maxIndex <= 2)"
            class="d-flex flex-nowrap conversation-viewer-row"
        >
            <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
            <div class="flex-fill conversation-items-separator"><hr class="dashed flex-fill" /></div>
        </div>
    </div>
</template>
<script>
import { BmContact } from "@bluemind/styleguide";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import MailConversationViewerVerticalLine from "./MailConversationViewerVerticalLine";
import MailViewerRecipients from "../MailViewerRecipients";

export default {
    name: "MailConversationViewerItem",
    components: { BmContact, MailConversationViewerVerticalLine, MailViewerRecipients },
    mixins: [MailConversationViewerItemMixin],
    props: {
        isDraft: {
            type: Boolean,
            default: false
        },
        stickyBottom: {
            type: Boolean,
            default: false
        }
    },
    methods: {
        collapse(event) {
            if (this.isMessageExpanded && !this.message.composing) {
                this.$emit("collapse");
                event.stopPropagation(); // needed to prevent expand event to be called then
            }
        },
        expand() {
            if (!this.isMessageExpanded) {
                this.$emit("expand");
            }
        },
        toggle(event) {
            if (!this.isMessageExpanded) {
                this.expand();
            } else {
                this.collapse(event);
            }
        }
    }
};
</script>
<style lang="scss">
@use "sass:math";
@import "@bluemind/styleguide/css/_variables.scss";

.mail-conversation-viewer-item {
    .avatar-wrapper {
        height: $input-height;
        padding-top: math.div($input-height - $avatar-height, 2);
    }
    hr {
        margin-top: 0;
        margin-bottom: 0;
        &.dashed {
            border-style: dashed;
        }
    }
    .contrast {
        background-color: $neutral-bg;
    }
    .mail-viewer-toolbar .btn {
        padding-top: 0;
        padding-bottom: 0;
    }
    .row.min-height {
        min-height: 2.5em;
    }
    .mail-conversation-viewer-item-body:focus {
        outline-offset: -0.375em;
    }
    .conversation-items-separator {
        padding-top: $sp-5 + $sp-3;
        padding-bottom: $sp-5;
    }
    .sticky-bottom {
        position: sticky;
        z-index: $zindex-sticky;
        bottom: 0;
    }
}
</style>
