<template>
    <chain-of-responsibility :is-responsible="activeFolder === MY_INBOX.key && quota.isAboveCriticalThreshold()">
        <section class="mail-home-screen mail-inbox-full-screen" aria-labelledby="text-1 text-2">
            <div class="starter-text-and-actions">
                <div class="starter-main">
                    <h1 id="text-1">{{ $t("mail.message.starter.display.inbox_full") }}</h1>
                    <div id="text-2">{{ $t("mail.message.starter.inbox_full.details") }}</div>
                    <bm-button
                        variant="fill"
                        size="lg"
                        icon="trash"
                        :to="{ name: 'v:mail:home', params: { folder: MY_TRASH.path } }"
                    >
                        {{ $t("mail.message.starter.display.trash") }}
                    </bm-button>
                </div>
                <div class="starter-links">
                    <bm-button
                        size="lg"
                        icon="pencil"
                        :to="{ name: 'v:mail:home', params: { folder: MY_DRAFTS.path } }"
                        variant="link"
                    >
                        {{ $t("mail.message.starter.display.drafts") }}
                    </bm-button>
                    <bm-button
                        size="lg"
                        icon="forbidden"
                        :to="{ name: 'v:mail:home', params: { folder: MY_JUNK.path } }"
                        variant="link"
                    >
                        {{ $t("mail.message.starter.display.junks") }}
                    </bm-button>
                </div>
            </div>
            <bm-illustration value="mailbox-full" size="lg" over-background />
            <active-folder-count class="after-illustration" />
        </section>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { MY_INBOX, MY_TRASH, MY_DRAFTS, MY_JUNK } from "~/getters";
import { BmButton, BmIllustration } from "@bluemind/ui-components";

import ChainOfResponsibility from "../../ChainOfResponsibility";
import ActiveFolderCount from "../ActiveFolderCount";

import { Quota } from "@bluemind/quota";

export default {
    name: "MailInboxFullScreen",
    components: { ActiveFolderCount, BmButton, BmIllustration, ChainOfResponsibility },
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", { MY_INBOX, MY_TRASH, MY_DRAFTS, MY_JUNK }),
        quota() {
            return new Quota(this.$store.state["root-app"].quota);
        }
    },

    priority: 129
};
</script>
