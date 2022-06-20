<template>
    <main class="flex-fill d-lg-flex flex-column mail-app">
        <bm-extension id="webapp.mail" path="app.header" />
        <global-events @click="showFolders = false" />
        <section
            :aria-label="$t('mail.application.region.mailtools')"
            class="row align-items-center shadow topbar z-index-250"
            :class="{ darkened }"
        >
            <bm-col
                cols="2"
                order="0"
                class="d-lg-flex justify-content-center"
                :class="hideListInResponsiveMode || composerOrMessageIsDisplayed || !SELECTION_IS_EMPTY ? 'd-none' : ''"
            >
                <bm-icon-button
                    variant="compact-on-fill-primary"
                    size="lg"
                    class="d-inline-block d-lg-none w-100"
                    icon="burger-menu"
                    @click.stop="showFolders = !showFolders"
                />
                <new-message v-if="activeFolder !== MY_TEMPLATES.key" />
                <new-template v-else />
            </bm-col>
            <bm-col
                cols="8"
                lg="3"
                order="1"
                class="d-lg-block px-2"
                :class="hideListInResponsiveMode || composerOrMessageIsDisplayed || !SELECTION_IS_EMPTY ? 'd-none' : ''"
            >
                <mail-search-form />
            </bm-col>
            <bm-col
                cols="2"
                lg="0"
                order="2"
                :class="composerOrMessageIsDisplayed || !SELECTION_IS_EMPTY ? 'd-none' : 'd-lg-none'"
            >
                <messages-options-for-mobile @shown="darkened = true" @hidden="darkened = false" />
            </bm-col>
            <bm-col
                :class="displayToolbarInResponsiveMode ? 'd-inline-block d-lg-block' : 'd-none'"
                class="h-100"
                cols="12"
                lg="5"
                order="2"
            >
                <mail-toolbar class="mx-3 mx-lg-0" />
            </bm-col>
            <bm-col v-if="canSwitchWebmail" order="last" class="d-none d-lg-block pr-5">
                <bm-form-checkbox
                    switch
                    left-label
                    checked="true"
                    class="switch-webmail text-right text-secondary"
                    @change="switchWebmail()"
                >
                    {{ $t("mail.main.switch.webmail") }}
                </bm-form-checkbox>
            </bm-col>
        </section>
        <bm-row class="flex-fill flex-nowrap mx-0">
            <!-- v-show is overridden by d-lg-block in large devices -->
            <section
                v-show="showFolders"
                :aria-label="$t('mail.application.region.folderlist')"
                class="folders-section position-lg-static position-absolute d-lg-block px-0 col-12 col-lg-2 overlay top-0 bottom-0"
            >
                <mail-folder-sidebar />
            </section>
            <multipane class="w-100" layout="vertical" @paneResizeStop="onPanelResize">
                <mail-conversation-list :class="{ 'd-none': hideListInResponsiveMode }" />
                <multipane-resizer />
                <div class="flex-grow-1 flex-basis-0 scroller-y scroller-visible-on-hover">
                    <router-view />
                </div>
            </multipane>
        </bm-row>
        <new-message mobile :class="hideListInResponsiveMode ? 'd-none' : 'd-block'" />
    </main>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import GlobalEvents from "vue-global-events";
import { BmExtension } from "@bluemind/extensions.vue";
import { inject } from "@bluemind/inject";
import BmRoles from "@bluemind/roles";
import { BmFormCheckbox, BmIconButton, BmCol, BmRow } from "@bluemind/styleguide";
import { Multipane, MultipaneResizer } from "@bluemind/vue-multipane";

import {
    ACTIVE_MESSAGE,
    CURRENT_CONVERSATION_METADATA,
    MY_TEMPLATES,
    SEVERAL_CONVERSATIONS_SELECTED,
    SELECTION_IS_EMPTY
} from "~/getters";
import { SET_WIDTH } from "~/mutations";

import FaviconHelper from "../FaviconHelper";
import UnreadCountScheduler from "./MailApp/UnreadCountScheduler";
import MailFolderSidebar from "./MailFolder/MailFolderSidebar";
import MailConversationList from "./MailConversationList/MailConversationList";
import MailToolbar from "./MailToolbar/";
import MailSearchForm from "./MailSearchForm";
import MessagesOptionsForMobile from "./MessagesOptionsForMobile";
import NewMessage from "./NewMessage";
import NewTemplate from "./NewTemplate";

import MailAppMixin from "./MailApp/MailAppMixin";

export default {
    name: "MailApp",
    components: {
        BmFormCheckbox,
        BmIconButton,
        BmCol,
        BmExtension,
        BmRow,
        GlobalEvents,
        MailFolderSidebar,
        MailConversationList,
        MailSearchForm,
        MailToolbar,
        MessagesOptionsForMobile,
        Multipane,
        MultipaneResizer,
        NewMessage,
        NewTemplate
    },
    mixins: [MailAppMixin, UnreadCountScheduler],
    data() {
        return {
            userSession: inject("UserSession"),
            showFolders: false,
            darkened: false,
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
        },
        composerOrMessageIsDisplayed() {
            return Boolean(this.ACTIVE_MESSAGE || this.currentConversation);
        },
        canSwitchWebmail() {
            return (
                this.userSession &&
                this.userSession.roles.includes(BmRoles.HAS_MAIL_WEBAPP) &&
                this.userSession.roles.includes(BmRoles.HAS_WEBMAIL)
            );
        },
        displayToolbarInResponsiveMode() {
            return this.composerOrMessageIsDisplayed || this.SEVERAL_CONVERSATIONS_SELECTED;
        }
    },
    created() {
        FaviconHelper.setFavicon();
        const documentTitle = this.$t("mail.application.title") + this.$t("common.product");
        document.title = documentTitle;
        FaviconHelper.handleUnreadNotifInFavicon(this.userSession, documentTitle);
    },
    methods: {
        onPanelResize(pane, container, size) {
            this.$store.commit("mail/" + SET_WIDTH, size);
        },
        async switchWebmail() {
            await inject("UserSettingsPersistence").setOne(this.userSession.userId, "mail-application", '"webmail"');
            location.replace("/webmail/");
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_type";
@import "~@bluemind/styleguide/css/_variables";
@import "~@bluemind/styleguide/css/_zIndex.scss";

.mail-app {
    .topbar {
        flex: none;
        height: base-px-to-rem(46);
        background-color: $fill-primary-bg;
        @include from-lg {
            background-color: $surface;
        }
    }
    .switch-webmail label {
        @extend %caption-bold;
        max-width: $custom-switch-width * 3;
        color: $secondary-fg;
        $switch-offset: math.div(2 * $line-height-small - $custom-switch-height, 2);
        $switch-indicator-offset: $switch-offset + math.div($custom-switch-height - $custom-switch-indicator-size, 2);
        &::before {
            top: $switch-offset !important;
        }
        &::after {
            top: $switch-indicator-offset !important;
        }
    }

    .folders-section {
        border-right: 1px solid $neutral-fg-lo1;
        z-index: 300;
        @include from-lg {
            z-index: 200;
        }
    }
    .mail-folder-sidebar-wrapper {
        background-color: $backdrop;
        @include until-lg {
            box-shadow: $box-shadow-lg;
        }
    }
    .darkened::before {
        position: fixed;
        content: "";
        background: $highest;
        top: 0;
        bottom: 0;
        width: 100%;
        opacity: 0.5;
        z-index: 1;
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

    .multipane {
        overflow-x: hidden;
    }

    .multipane-resizer {
        @extend .z-index-110;
        visibility: hidden;
    }

    @media only screen {
        @include from-lg {
            .mail-conversation-list-wrapper {
                min-width: 20%;
                max-width: 70%;
                width: 30%;
            }
            .layout-v > .multipane-resizer {
                visibility: visible;
                margin-left: -$sp-1 * 0.5;
                left: 0;
                width: $sp-1;
                min-width: $sp-1;

                &:active {
                    margin-left: -$sp-1;
                    border-right: ($sp-1 * 0.25) $neutral-fg solid;
                    width: $sp-1 * 1.25;
                    min-width: $sp-1 * 1.25;
                }
            }
        }
    }
}
.flex-basis-0 {
    flex-basis: 0;
}

@media print {
    .mail-app {
        .topbar {
            display: none !important;
        }
    }
}
</style>
