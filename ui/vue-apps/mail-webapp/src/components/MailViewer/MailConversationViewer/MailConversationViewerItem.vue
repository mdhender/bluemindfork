<template>
    <div class="mail-conversation-viewer-item">
        <div
            class="mail-conversation-viewer-item-body"
            :class="{ 'last-before-draft': isLastBeforeDraft }"
            tabindex="0"
            @click.prevent="expand"
            @keypress.enter="toggle"
        >
            <div class="row pl-5">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" />
                <div class="col spacer" />
            </div>
            <div class="row min-height pl-5 click-to-collapse-zone flex-nowrap" @click="collapse">
                <div
                    class="col-1 d-flex align-items-center vertical-line"
                    :class="{ first: index === 0, last: index === maxIndex }"
                >
                    <bm-contact :contact="message.from" no-text />
                </div>
                <slot name="head" />
            </div>
            <slot name="subhead" />
            <div
                v-if="isMessageExpanded && !message.composing"
                class="row pr-3 pl-5 click-to-collapse-zone"
                @click="collapse"
            >
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <mail-viewer-recipients :message="message" class="col-11 px-3" @click.native.stop />
            </div>
            <div class="row pl-5">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <slot name="content" />
            </div>
            <div class="row pl-5" :class="{ 'sticky-bottom': stickyBottom }">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <slot name="bottom" />
            </div>
        </div>

        <div v-if="nextIsDraft || (isDraft && index !== maxIndex)" class="row contrast">
            <div class="col spacer" />
        </div>
        <div v-else-if="(!nextIsHidden || index === maxIndex || maxIndex <= 2)" class="row pl-5">
            <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
            <div class="col pl-3 py-0"><hr class="dashed" /></div>
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
@import "@bluemind/styleguide/css/_variables.scss";

.mail-conversation-viewer-item {
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
    .sticky-bottom {
        position: sticky;
        z-index: $zindex-sticky;
        bottom: 0;
    }
}
</style>
