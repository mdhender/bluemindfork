<template>
    <div class="mail-message-starter mt-5 text-center font-size-lg">
        <div class="m-auto d-block position-relative">
            <svg
                version="1.1"
                xmlns="http://www.w3.org/2000/svg"
                width="100%"
                height="100%"
                viewBox="0 0 174 200"
                class="hexagon position-relative"
            >
                <polygon points="87,0 174,50 174,150 87,200 0,150 0,50 87,0" />
            </svg>
            <div class="mail-message-starter-content position-absolute">
                <h1>
                    <bm-icon icon="emoticon" class="mb-3" size="lg" /> <br>
                    <i18n path="mail.message.starter" tag="span">
                        <br place="cr">
                    </i18n>
                </h1>
                <hr class="my-4">
                <div>
                    {{ $t("mail.message.starter.write") }} <br>
                    <bm-button :to="{ path: 'new' }" variant="primary">
                        <bm-label-icon icon="plus">{{ $t("mail.main.new") }}</bm-label-icon>
                    </bm-button>
                </div>
                <div>
                    {{ $t("mail.message.starter.display") }}<br>
                    <bm-button
                        variant="secondary"
                        :to="{ path: '/mail/' + folder + '/' + firstUnreadMessage }"
                        :disabled="!firstUnreadMessage"
                    >
                        <bm-label-icon icon="unread">
                            {{ $t("mail.message.starter.display.unread_message") }}
                        </bm-label-icon>
                    </bm-button>
                </div>
                <div>
                    {{ $t("mail.message.starter.display") }} <br>
                    <bm-button :to="{ path: '/mail/' + getDraftUid() }" variant="secondary">
                        <bm-label-icon icon="pencil">
                            {{ $t("mail.message.starter.display.drafts") }}
                        </bm-label-icon>
                    </bm-button>
                </div>
            </div>
        </div>
        <img src="images/logo-bluemind.png" class="mt-4">
    </div>
</template>

<script>
import BmButton from "@bluemind/styleguide/components/buttons/BmButton";
import BmIcon from "@bluemind/styleguide/components/BmIcon";
import BmLabelIcon from "@bluemind/styleguide/components/BmLabelIcon";
import { mapGetters } from "vuex";

export default {
    name: "MailMessageStarter",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon
    },
    computed: {
        ...mapGetters("backend.mail/folders", ["tree"]),
        ...mapGetters("backend.mail/items", ["messages"]),
        ...mapGetters("backend.mail/folders", { folder: "currentFolder" }),

        firstUnreadMessage() {
            if (this.folder) {
                const message = this.messages.find(message => message.states.includes("not-seen"));
                if (message) {
                    return message.uid;
                }
            }
            return null;
        }
    },
    methods: {
        getDraftUid() {
            if (this.tree.length > 0) {
                return this.tree.find(folder => folder.name === "Drafts").uid;
            }
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-message-starter {
    font-size: $font-size-lg;
}

.mail-message-starter .mail-message-starter-content > div {
    margin-bottom: map-get($spacers, 4);
    line-height: 2;
}

.mail-message-starter h1 {
    color: $info-dark;
}

.mail-message-starter svg.hexagon polygon {
    fill: $surface-bg;
}

.mail-message-starter .mail-message-starter-content {
    top: 3em;
    left: 4em;
    width: 27em;
}

.mail-message-starter > div {
    width: 35em;
}
</style>
