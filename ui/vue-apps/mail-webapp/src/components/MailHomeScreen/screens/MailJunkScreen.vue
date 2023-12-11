<template>
    <chain-of-responsibility :is-responsible="activeFolder === MY_JUNK.key">
        <section class="mail-home-screen mail-junk-screen" aria-labelledby="text-1 text-2">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ $t("mail.message.starter") }}</h1>
                    <div id="text-2">{{ $t("common.or") }}</div>
                    <empty-folder-action v-slot="action">
                        <bm-button variant="fill-danger" size="lg" icon="broom" @click="action.execute">
                            {{ $t("mail.actions.empty_folder.label") }}
                        </bm-button>
                    </empty-folder-action>
                </div>
                <div class="starter-links">
                    <div class="starter-link">
                        <bm-button
                            size="lg"
                            icon="inbox"
                            :to="{ name: 'v:mail:home', params: { folder: MY_INBOX.path } }"
                            variant="link"
                        >
                            {{ $t("mail.message.starter.display.inbox") }}
                        </bm-button>
                    </div>
                </div>
            </div>
            <bm-illustration value="spam" size="lg" over-background />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { MY_INBOX, MY_JUNK } from "~/getters";
import { BmButton, BmIcon, BmIllustration } from "@bluemind/ui-components";

import EmptyFolderAction from "~/components/MailFolder/EmptyFolderAction";
import ChainOfResponsibility from "../../ChainOfResponsibility";

export default {
    name: "MailJunkScreen",
    components: { ChainOfResponsibility, EmptyFolderAction, BmButton, BmIllustration },
    computed: {
        ...mapState("mail", ["activeFolder"]),
        ...mapGetters("mail", { MY_INBOX, MY_JUNK })
    },

    priority: 128
};
</script>
