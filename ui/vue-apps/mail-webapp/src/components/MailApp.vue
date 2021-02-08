<template>
    <main class="flex-fill d-lg-flex flex-column mail-app">
        <global-events @click="showFolders = false" />
        <section
            :aria-label="$t('mail.application.region.mailtools')"
            class="row align-items-center shadow-sm bg-surface topbar z-index-250"
            :class="{ darkened }"
        >
            <bm-col
                cols="2"
                order="0"
                class="d-lg-flex justify-content-start pl-lg-4"
                :class="hideListInResponsiveMode || composerOrMessageIsDisplayed ? 'd-none' : ''"
            >
                <bm-button variant="inline-light" class="d-inline-block d-lg-none w-100" @click.stop="toggleFolders">
                    <bm-icon icon="burger-menu" size="2x" />
                </bm-button>
                <new-message />
            </bm-col>
            <bm-col
                cols="8"
                lg="3"
                order="1"
                class="d-lg-block px-2"
                :class="hideListInResponsiveMode || composerOrMessageIsDisplayed ? 'd-none' : ''"
            >
                <mail-search-form />
            </bm-col>
            <bm-col cols="2" lg="0" order="2" :class="composerOrMessageIsDisplayed ? 'd-none' : 'd-lg-none'">
                <messages-options-for-mobile @shown="darkened = true" @hidden="darkened = false" />
            </bm-col>
            <bm-col
                :class="displayToolbarInResponsiveMode ? 'd-inline-block d-lg-block' : 'd-none'"
                class="h-100"
                cols="12"
                lg="5"
                order="2"
            >
                <mail-toolbar class="mx-auto mx-lg-0" />
            </bm-col>
            <bm-col v-if="canSwitchWebmail" order="last" class="d-none d-lg-block pr-2">
                <bm-form-checkbox
                    switch
                    checked="true"
                    class="switch-webmail text-condensed text-right"
                    @change="switchWebmail()"
                >
                    {{ $t("mail.main.switch.webmail") }}
                </bm-form-checkbox>
            </bm-col>
        </section>
        <bm-row class="flex-fill flex-nowrap">
            <!-- v-show is overridden by d-lg-block in large devices -->
            <section
                v-show="showFolders"
                :aria-label="$t('mail.application.region.folderlist')"
                class="folders-section row position-lg-static position-absolute d-lg-block px-0 col-12 col-lg-2 overlay top-0 bottom-0"
            >
                <mail-folder-sidebar @toggle-folders="toggleFolders" />
            </section>
            <multipane class="w-100" layout="vertical">
                <div
                    class="pl-lg-2 px-0 d-lg-block mail-message-list-div"
                    :class="hideListInResponsiveMode ? 'd-none' : ''"
                >
                    <mail-message-list class="h-100" />
                </div>
                <multipane-resizer />
                <div class="flex-grow-1 overflow-auto flex-basis-0">
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
import { BmFormCheckbox, BmButton, BmCol, BmIcon, BmRow, MakeUniq } from "@bluemind/styleguide";
import { inject } from "@bluemind/inject";

import FaviconHelper from "../FaviconHelper";
import BoostrapMixin from "./BootstrapMixin";
import RouterMixin from "./RouterMixin";
import MailAppL10N from "../../l10n/";
import MailFolderSidebar from "./MailFolder/MailFolderSidebar";
import MailMessageList from "./MailMessageList/MailMessageList";
import MailToolbar from "./MailToolbar/";
import MailSearchForm from "./MailSearchForm";
import MessagesOptionsForMobile from "./MessagesOptionsForMobile";
import NewMessage from "./NewMessage";
import { MULTIPLE_MESSAGE_SELECTED } from "~getters";
import { Multipane, MultipaneResizer } from "@bluemind/vue-multipane";

export default {
    name: "MailApp",
    components: {
        BmFormCheckbox,
        BmButton,
        BmCol,
        BmIcon,
        BmRow,
        GlobalEvents,
        MailFolderSidebar,
        MailMessageList,
        MailSearchForm,
        MailToolbar,
        MessagesOptionsForMobile,
        Multipane,
        MultipaneResizer,
        NewMessage
    },
    mixins: [MakeUniq, BoostrapMixin, RouterMixin],
    componentI18N: { messages: MailAppL10N },
    data() {
        return {
            userSession: inject("UserSession"),
            showFolders: false,
            darkened: false,
            unreadNotifInfavicon: 0
        };
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["messages", "selection"]),
        ...mapGetters("mail", { MULTIPLE_MESSAGE_SELECTED }),
        isMessageComposerDisplayed() {
            return this.currentMessageKey && this.messages[this.currentMessageKey]
                ? this.messages[this.currentMessageKey].composing
                : false;
        },
        hideListInResponsiveMode() {
            return this.isMessageComposerDisplayed || (this.currentMessageKey && this.selection.length === 0);
        },
        composerOrMessageIsDisplayed() {
            return this.isMessageComposerDisplayed || !!this.currentMessageKey;
        },
        canSwitchWebmail() {
            return (
                this.userSession &&
                this.userSession.roles.includes("hasMailWebapp") &&
                this.userSession.roles.includes("hasWebmail")
            );
        },
        displayToolbarInResponsiveMode() {
            return this.composerOrMessageIsDisplayed || this.MULTIPLE_MESSAGE_SELECTED;
        }
    },
    created() {
        FaviconHelper.setFavicon();
        const documentTitle = this.$t("mail.application.title") + " - Bluemind";
        document.title = documentTitle;
        FaviconHelper.handleUnreadNotifInFavicon(this.userSession, documentTitle);
    },
    methods: {
        toggleFolders() {
            this.showFolders = !this.showFolders;
        },
        async switchWebmail() {
            await inject("UserSettingsPersistence").setOne(this.userSession.userId, "mail-application", '"webmail"');
            location.replace("/webmail/");
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
@import "~@bluemind/styleguide/css/_zIndex.scss";

.mail-app {
    .topbar {
        flex: 0 0 4em;
        @media (max-width: map-get($grid-breakpoints, "lg")) {
            background-color: $info-dark;

            .btn-simple-dark {
                background-color: none;
                color: $light;
            }
        }
    }
    .switch-webmail label {
        max-width: $custom-switch-width * 3;
        &::before {
            top: $custom-control-indicator-size / 2 !important;
        }
        &::after {
            top: #{($custom-switch-indicator-size + $custom-control-indicator-size) / 2} !important;
        }
    }

    .folders-section {
        z-index: 200;
        @media (max-width: map-get($grid-breakpoints, "lg")) {
            z-index: 300;
        }
    }
    .mail-folder-sidebar-wrapper {
        @media (max-width: map-get($grid-breakpoints, "lg")) {
            box-shadow: $box-shadow-lg;
        }
    }
    .darkened::before {
        position: absolute;
        content: "";
        background: black;
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

    .mail-message-list-div {
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

    /* Large devices (laptops/desktops, 992px and up) */
    @media only screen and (min-width: 992px) {
        .mail-message-list-div {
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
                border-right: ($sp-1 * 0.25) $dark solid;
                width: $sp-1 * 1.25;
                min-width: $sp-1 * 1.25;
            }
        }
    }
}

.flex-basis-0 {
    flex-basis: 0;
}
</style>
