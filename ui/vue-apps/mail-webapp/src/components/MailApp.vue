<template>
    <main class="flex-fill d-lg-flex flex-column mail-app">
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
                <bm-button variant="inline-light" class="d-inline-block d-lg-none w-100" @click="toggleFolders">
                    <bm-icon icon="burger-menu" size="2x" />
                </bm-button>
                <bm-button variant="primary" class="text-nowrap d-lg-inline-block d-none" @click="composeNewMessage">
                    <bm-label-icon icon="plus">{{ $t("mail.main.new") }}</bm-label-icon>
                </bm-button>
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
        <bm-row class="flex-fill position-relative flex-nowrap">
            <!-- v-show is overridden by d-lg-block in large devices -->
            <section
                v-show="showFolders"
                :aria-label="$t('mail.application.region.folderlist')"
                class="row position-lg-static position-absolute d-lg-block px-0 col-12 col-lg-2 z-index-200 overlay top-0 bottom-0"
            >
                <bm-col cols="10" lg="12" class="mail-folder-sidebar-wrapper bg-surface h-100">
                    <mail-folder-sidebar @toggle-folders="toggleFolders" />
                </bm-col>
            </section>
            <bm-col cols="12" lg="3" class="pl-lg-2 px-0 d-lg-block" :class="hideListInResponsiveMode ? 'd-none' : ''">
                <mail-message-list class="h-100" />
            </bm-col>
            <bm-col lg="7" class="overflow-auto d-flex flex-column">
                <router-view />
            </bm-col>
        </bm-row>
        <bm-button
            v-bm-clip-path="'hexagon'"
            variant="primary"
            class="d-lg-none position-absolute mail-app-responsive-btn z-index-110"
            :class="hideListInResponsiveMode ? 'd-none' : 'd-block'"
            @click="composeNewMessage"
        >
            <bm-icon icon="plus" size="2x" />
        </bm-button>
    </main>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";

import {
    BmFormCheckbox,
    BmLabelIcon,
    BmButton,
    BmCol,
    BmIcon,
    BmRow,
    MakeUniq,
    BmClipPath
} from "@bluemind/styleguide";
import { inject } from "@bluemind/inject";

import FaviconHelper from "../FaviconHelper";
import MailAppL10N from "../../l10n/";
import MailFolderSidebar from "./MailFolder/MailFolderSidebar";
import MailMessageList from "./MailMessageList/MailMessageList";
import MailToolbar from "./MailToolbar/";
import MailSearchForm from "./MailSearchForm";
import MessagesOptionsForMobile from "./MessagesOptionsForMobile";
import actionTypes from "../store/actionTypes";
import { MessageCreationModes } from "../model/message";

export default {
    name: "MailApp",
    components: {
        BmFormCheckbox,
        BmButton,
        BmCol,
        BmLabelIcon,
        BmIcon,
        BmRow,
        MailFolderSidebar,
        MailMessageList,
        MailSearchForm,
        MailToolbar,
        MessagesOptionsForMobile
    },
    directives: { BmClipPath },
    mixins: [MakeUniq],
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
        ...mapState("mail-webapp", ["selectedMessageKeys", "messageFilter"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", ["messages"]),
        ...mapGetters("mail", ["MY_DRAFTS"]),
        isMessageComposerDisplayed() {
            return this.currentMessageKey && this.messages[this.currentMessageKey]
                ? this.messages[this.currentMessageKey].composing
                : false;
        },
        hideListInResponsiveMode() {
            return this.isMessageComposerDisplayed || (this.currentMessageKey && this.selectedMessageKeys.length === 0);
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
            return this.composerOrMessageIsDisplayed || this.selectedMessageKeys.length > 1;
        }
    },
    created() {
        FaviconHelper.setFavicon();
        const documentTitle = this.$t("mail.application.title") + " - Bluemind";
        document.title = documentTitle;
        FaviconHelper.handleUnreadNotifInFavicon(this.userSession, documentTitle);
    },
    methods: {
        ...mapActions("mail", [actionTypes.CREATE_MESSAGE]),
        async composeNewMessage() {
            const messageKey = await this.CREATE_MESSAGE({
                myDraftsFolder: this.MY_DRAFTS,
                creationMode: MessageCreationModes.NEW
            });
            return this.$router.navigate({ name: "v:mail:message", params: { message: messageKey } });
        },
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
    .mail-folder-sidebar-wrapper {
        @media (max-width: map-get($grid-breakpoints, "lg")) {
            box-shadow: $box-shadow-lg;
        }
    }
    .darkened::before {
        position: absolute;
        content: "";
        background: black;
        top: 0px;
        bottom: 0px;
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
}
</style>
