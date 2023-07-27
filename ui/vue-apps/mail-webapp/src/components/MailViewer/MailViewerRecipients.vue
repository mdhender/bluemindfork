<template>
    <div class="mail-viewer-recipients" @keyup.esc.prevent.stop="toShowMore = false">
        <mail-viewer-recipient
            v-if="message.to.length"
            id="mail-viewer-recipient-to"
            :recipients="message.to"
            @show-more="
                ccShowMore = false;
                bccShowMore = false;
                toShowMore = !toShowMore;
            "
            >{{ $t("common.to") }}</mail-viewer-recipient
        >
        <mail-viewer-recipient
            v-if="message.cc.length"
            id="mail-viewer-recipient-cc"
            :recipients="message.cc"
            @show-more="
                toShowMore = false;
                ccShowMore = false;
                ccShowMore = !ccShowMore;
            "
            >{{ $t("common.cc") }}</mail-viewer-recipient
        >
        <mail-viewer-recipient
            v-if="message.bcc.length"
            id="mail-viewer-recipient-bcc"
            :recipients="message.bcc"
            @show-more="
                toShowMore = false;
                ccShowMore = false;
                bccShowMore = !bccShowMore;
            "
            >{{ $t("common.bcc") }}</mail-viewer-recipient
        >
        <bm-popover
            v-if="message.to.length"
            target="mail-viewer-recipient-to"
            triggers="manuel"
            :show.sync="toShowMore"
            placement="bottom"
            custom-class="recipients-popover"
            no-fade
        >
            <mail-viewer-recipients-more-content :message="message" @close="toShowMore = false" />
        </bm-popover>
        <bm-popover
            v-if="message.cc.length"
            target="mail-viewer-recipient-cc"
            triggers="manuel"
            :show.sync="ccShowMore"
            placement="bottom"
            custom-class="recipients-popover"
            no-fade
        >
            <mail-viewer-recipients-more-content :message="message" @close="ccShowMore = false" />
        </bm-popover>
        <bm-popover
            v-if="message.bcc.length"
            target="mail-viewer-recipient-bcc"
            triggers="manuel"
            :show.sync="bccShowMore"
            placement="bottom"
            custom-class="recipients-popover"
            no-fade
        >
            <mail-viewer-recipients-more-content :message="message" @close="bccShowMore = false" />
        </bm-popover>
        <bm-modal
            v-if="message.to.length"
            v-model="toShowMore"
            size="sm"
            height="lg"
            modal-class="mail-viewer-recipients-modal"
            hide-footer
            hide-header
        >
            <mail-viewer-recipients-more-content :message="message" hide-close />
        </bm-modal>
        <bm-modal
            v-if="message.cc.length"
            v-model="ccShowMore"
            size="sm"
            height="lg"
            modal-class="mail-viewer-recipients-modal"
            hide-footer
            hide-header
        >
            <mail-viewer-recipients-more-content :message="message" hide-close />
        </bm-modal>
        <bm-modal
            v-if="message.bcc.length"
            v-model="bccShowMore"
            size="sm"
            height="lg"
            modal-class="mail-viewer-recipients-modal"
            hide-footer
            hide-header
        >
            <mail-viewer-recipients-more-content :message="message" hide-close />
        </bm-modal>
    </div>
</template>

<script>
import { BmModal, BmPopover } from "@bluemind/ui-components";
import MailViewerRecipient from "./MailViewerRecipient";
import MailViewerRecipientsMoreContent from "./MailViewerRecipientsMoreContent";

export default {
    name: "MailViewerRecipients",
    components: { BmModal, BmPopover, MailViewerRecipient, MailViewerRecipientsMoreContent },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { bccShowMore: false, ccShowMore: false, toShowMore: false };
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-viewer-recipients {
    display: flex;
    flex-direction: column;
    gap: $sp-3;
}

.mail-viewer-recipients-modal {
    @include from-lg {
        display: none !important;
    }
    .modal-body {
        padding: 0 !important;
    }
    .mail-viewer-recipients-more-content {
        height: 100%;
    }
}

.recipients-popover {
    @include until-lg {
        display: none !important;
    }

    max-width: 50vw !important;
    min-width: $popover-min-width;

    .mail-viewer-recipients-more-content {
        max-height: 65vh;
    }

    .popover-body {
        padding: 0;
    }

    .arrow {
        display: none !important;
    }
}

@include from-lg {
    .recipients-popover + div > .modal-backdrop {
        display: none;
    }
}
</style>
