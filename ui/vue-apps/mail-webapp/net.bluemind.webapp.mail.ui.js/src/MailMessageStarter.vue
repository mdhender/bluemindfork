<template>
    <div class="mail-message-starter h-100 d-none d-md-flex">
        <div class="d-flex flex-column justify-content-center h-100 text-center w-100">
            <div class="d-flex flex-grow-1 flex-shrink-0 flex-column justify-space-evenly">
                <h1>{{ $t("mail.message.starter") }}</h1>
                <h1>{{ $t("common.or") }}</h1>
            </div>
            <div class="flex-grow-1 flex-shrink-0 overflow-hidden d-flex flex-column align-items-center">
                <div class="bg-white py-3 d-table ">
                    <div class="d-table-cell px-4">
                        <div class="pb-2">{{ $t("mail.message.starter.write") }}</div>
                        <bm-button :to="{ name: 'mail:new' }" variant="primary">
                            <bm-label-icon icon="plus"> {{ $t("mail.main.new") }} </bm-label-icon>
                        </bm-button>
                    </div>
                    <div class="d-table-cell px-4">
                        <div class="pb-2">{{ $t("mail.message.starter.display") }}</div>
                        <bm-button
                            v-if="my.DRAFTS"
                            :to="{ name: 'v:mail:home', params: { folder: my.DRAFTS.value.fullName } }"
                            variant="secondary"
                        >
                            <bm-label-icon icon="pencil">
                                {{ $t("mail.message.starter.display.drafts") }}
                            </bm-label-icon>
                        </bm-button>
                    </div>
                </div>
            </div>
            <div
                class="w-100 flex-shrink-1"
                :style="'flex-basis: 321px;background: url(' + emptyMessageIllustration + ') no-repeat center top'"
            />
        </div>
    </div>
</template>

<script>
import { BmButton, BmLabelIcon } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import emptyMessageIllustration from "../assets/home-page.png";

export default {
    name: "MailMessageStarter",
    components: {
        BmButton,
        BmLabelIcon
    },
    data() {
        return {
            emptyMessageIllustration
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["tree", "my"]),
        ...mapGetters("mail-webapp/messages", ["messages"]),
        ...mapState("mail-webapp", ["currentFolderKey"]),

        firstUnreadMessage() {
            if (this.currentFolderKey) {
                const message = this.messages.find(message => message && message.states.includes("not-seen"));
                if (message) {
                    return message.key;
                }
            }
            return null;
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-message-starter {
    font-size: $font-size-lg;
}

.mail-message-starter h1 {
    color: $info-dark;
    font-size: 2rem;
}

.mail-message-starter svg.hexagon polygon {
    fill: $surface-bg;
}

.mail-message-starter .flex-grow-3 {
    flex-grow: 3;
}

.mail-message-starter .justify-space-evenly {
    justify-content: space-evenly;
}
</style>
