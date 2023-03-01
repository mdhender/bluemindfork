<template>
    <chain-of-responsibility :is-responsible="!isMyMailbox">
        <section class="mail-home-screen minimalist mail-shared-folder-screen" aria-labelledby="text-1">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ $t("mail.message.starter") }}</h1>
                    <div class="shared-folder">
                        <mail-mailbox-icon v-if="CURRENT_MAILBOX" :mailbox="CURRENT_MAILBOX" />
                        <div class="text">
                            {{ $t("mail.message.starter.shared.info") }}
                        </div>
                    </div>
                </div>
            </div>
            <bm-illustration value="folder-shared" size="lg" over-background />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { CURRENT_MAILBOX, MY_MAILBOX } from "~/getters";
import { BmIllustration } from "@bluemind/ui-components";

import MailMailboxIcon from "../../MailMailboxIcon";
import ChainOfResponsibility from "../ChainOfResponsibility";

export default {
    name: "MailSharedFolderScreen",
    components: { MailMailboxIcon, ChainOfResponsibility, BmIllustration },
    computed: {
        ...mapState("mail", ["activeFolder"]),
        ...mapGetters("mail", { CURRENT_MAILBOX, MY_MAILBOX }),

        isMyMailbox() {
            return this.CURRENT_MAILBOX?.key === this.MY_MAILBOX.key;
        }
    },

    priority: 128
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/type";
@import "~@bluemind/ui-components/src/css/variables";

.mail-shared-folder-screen {
    .shared-folder {
        @extend %bold;
        display: flex;
        gap: $sp-4;

        & > .text {
            margin-top: base-px-to-rem(3);
            text-align: start;
        }
    }
}
</style>
