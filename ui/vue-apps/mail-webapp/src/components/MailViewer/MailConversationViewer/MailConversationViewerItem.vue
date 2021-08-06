<template>
    <div class="mail-conversation-viewer-item">
        <div
            class="mail-conversation-viewer-item-body"
            :class="{ 'last-before-draft': isLastBeforeDraft }"
            tabindex="0"
            @click.prevent="$emit('expand')"
            @keypress.enter="$emit('expand')"
        >
            <div class="row pl-5">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" />
                <div class="col spacer" />
            </div>
            <div class="row min-height pl-5">
                <div
                    class="col-1 d-flex align-items-center vertical-line"
                    :class="{ first: index === 0, last: index === maxIndex }"
                >
                    <bm-avatar :alt="message.from ? message.from.dn || message.from.address : ''" />
                </div>
                <slot name="head" />
            </div>
            <slot name="subhead" />
            <div v-if="index !== 0" class="row pr-3 pl-5">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <mail-viewer-recipients :message="message" class="px-3" />
            </div>
            <div class="row pl-5">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <slot name="content" />
            </div>
            <div class="row pl-5">
                <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
                <slot name="bottom" />
            </div>
        </div>

        <div v-if="nextIsDraft || (isDraft && index !== maxIndex)" class="row bg-light">
            <div class="col spacer" />
        </div>
        <div v-else-if="(!nextIsHidden || index === maxIndex || maxIndex <= 2)" class="row pl-5">
            <mail-conversation-viewer-vertical-line :index="index" :max-index="maxIndex" after-avatar />
            <div class="col pl-3 py-0"><hr class="dashed" /></div>
        </div>
    </div>
</template>
<script>
import { BmAvatar } from "@bluemind/styleguide";
import MailConversationViewerItemMixin from "./MailConversationViewerItemMixin";
import MailConversationViewerVerticalLine from "./MailConversationViewerVerticalLine";
import MailViewerRecipients from "../MailViewerRecipients";

export default {
    name: "MailConversationViewerItem",
    components: { BmAvatar, MailConversationViewerVerticalLine, MailViewerRecipients },
    mixins: [MailConversationViewerItemMixin]
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
    .mail-viewer-toolbar .btn {
        padding-top: 0;
        padding-bottom: 0;
    }
    .row.min-height {
        min-height: 2.5em;
    }
    .text-alternate-light {
        color: $alternate-light;
    }
    .mail-conversation-viewer-item-body:focus {
        outline-offset: -0.375em;
    }
}
</style>
