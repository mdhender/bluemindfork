<template>
    <main class="flex-fill flex-column mail-app">
        <bm-extension id="webapp.mail" path="app.header" />
        <global-events @click="showFolders = false" />
        <section :aria-label="$t('mail.application.region.mailtools')" class="z-index-250">
            <topbar class="d-flex align-items-center" @showFolders="showFolders = true" />
        </section>
        <bm-row class="flex-fill flex-nowrap mx-0">
            <!-- v-show is overridden by d-lg-block in large devices -->
            <section
                v-show="showFolders"
                :aria-label="$t('mail.application.region.folderlist')"
                class="folders-section position-lg-static position-absolute d-lg-block px-0 col-12 col-lg-2 top-0 bottom-0"
            >
                <mail-folder-sidebar />
            </section>
            <bm-multipane class="flex-fill" layout="vertical" @paneResizeStop="onPanelResize">
                <mail-conversation-list :class="{ 'd-none': hideListInResponsiveMode }" />
                <bm-multipane-resizer class="d-none d-lg-flex" />
                <div class="flex-grow-1 flex-basis-0 scroller-y right-panel">
                    <router-view />
                </div>
            </bm-multipane>
        </bm-row>
    </main>
</template>

<script>
import { mapGetters, mapState } from "vuex";

import GlobalEvents from "vue-global-events";
import { BmExtension } from "@bluemind/extensions.vue";
import { inject } from "@bluemind/inject";
import { BmRow, BmMultipane, BmMultipaneResizer } from "@bluemind/ui-components";

import {
    ACTIVE_MESSAGE,
    CURRENT_CONVERSATION_METADATA,
    MY_TEMPLATES,
    SEVERAL_CONVERSATIONS_SELECTED,
    SELECTION_IS_EMPTY
} from "~/getters";
import { SET_WIDTH } from "~/mutations";
import MailStore from "../store/";

import FaviconHelper from "../FaviconHelper";
import UnreadCountScheduler from "./MailApp/UnreadCountScheduler";
import MailConversationList from "./MailConversationList/MailConversationList";

import MailAppMixin from "./MailApp/MailAppMixin";
import Topbar from "./MailTopbar/Topbar";
import MailFolderSidebar from "./MailFolder/MailFolderSidebar";

export default {
    name: "MailApp",
    components: {
        BmExtension,
        BmMultipane,
        BmMultipaneResizer,
        BmRow,
        GlobalEvents,
        MailConversationList,
        Topbar,
        MailFolderSidebar
    },
    mixins: [MailAppMixin, UnreadCountScheduler],
    data() {
        return {
            userSession: inject("UserSession"),
            showFolders: false,
            unreadNotifInfavicon: 0
        };
    },
    computed: {
        ...mapGetters("mail", {
            ACTIVE_MESSAGE,
            MY_TEMPLATES,
            CURRENT_CONVERSATION_METADATA,
            SEVERAL_CONVERSATIONS_SELECTED,
            SELECTION_IS_EMPTY
        }),
        ...mapState("mail", { currentConversation: ({ conversations }) => conversations.currentConversation }),
        ...mapState("mail", ["activeFolder"]),
        hideListInResponsiveMode() {
            const item = this.ACTIVE_MESSAGE || this.CURRENT_CONVERSATION_METADATA;
            return item && (item.composing || this.SELECTION_IS_EMPTY);
        }
    },
    beforeCreate() {
        if (!this.$store.hasModule("mail")) {
            this.$store.registerModule("mail", MailStore);
        }
    },
    created() {
        FaviconHelper.initFavicon(this.userSession, document.title);
    },
    destroyed() {
        if (this.$store.hasModule("mail")) {
            this.$store.unregisterModule("mail");
        }
    },
    methods: {
        onPanelResize(pane, container, size) {
            this.$store.commit("mail/" + SET_WIDTH, size);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-app {
    .folders-section {
        background-color: $modal-backdrop;
        z-index: 300;
        @include from-lg {
            background-color: $surface;
            border-right: $separator-thickness solid $separator-color;
            z-index: 200;
        }
    }
    .mail-folder-sidebar-wrapper {
        background-color: $backdrop;
        @include until-lg {
            box-shadow: $box-shadow-lg;
        }
    }

    .mail-app-responsive-btn {
        bottom: $sp-2;
        right: $sp-2;
        height: 4em;
        width: 4em;
    }

    .mail-conversation-list-wrapper {
        min-width: 100%;
        width: 100%;
    }

    @media only screen {
        @include from-lg {
            .mail-conversation-list-wrapper {
                min-width: 20%;
                max-width: 70%;
                width: 30%;
            }
            .right-panel {
                scroll-padding-bottom: base-px-to-rem(200);
            }
        }
        @include until-lg {
            .right-panel {
                scroll-padding-bottom: base-px-to-rem(300);
            }
        }
    }
    .flex-basis-0 {
        flex-basis: 0;
    }
}
</style>
