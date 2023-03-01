<template>
    <chain-of-responsibility :is-responsible="isMyMailbox">
        <section class="mail-home-screen minimalist mail-folder-screen" aria-labelledby="text-1">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ $t("mail.message.starter") }}</h1>
                </div>
            </div>
            <bm-illustration value="folder" size="lg" over-background />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { CURRENT_MAILBOX, MY_MAILBOX } from "~/getters";
import { BmIllustration } from "@bluemind/ui-components";

import ChainOfResponsibility from "../ChainOfResponsibility";

export default {
    name: "MailFolderScreen",
    components: { ChainOfResponsibility, BmIllustration },
    computed: {
        ...mapState("mail", ["folders", "activeFolder"]),
        ...mapGetters("mail", { CURRENT_MAILBOX, MY_MAILBOX }),

        isMyMailbox() {
            return this.CURRENT_MAILBOX?.key === this.MY_MAILBOX.key;
        },
        folderName() {
            return this.folders[this.activeFolder]?.name;
        }
    },

    priority: 125
};
</script>
