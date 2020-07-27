<template>
    <div class="flex-fill d-flex flex-column mail-app">
        <bm-row align-v="center" class="shadow-sm bg-surface topbar z-index-250" :class="{ darkened }">
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
            <bm-col v-if="canSwitchWebmail" order="last" class="d-none d-lg-block pr-4">
                <bm-form-checkbox
                    switch
                    checked="true"
                    class="switch-webmail text-condensed text-right"
                    @change="switchWebmail()"
                >
                    {{ $t("mail.main.switch.webmail") }}
                </bm-form-checkbox>
            </bm-col>
        </bm-row>
        <bm-row class="flex-fill position-relative flex-nowrap">
            <!-- v-show is overridden by d-lg-block in large devices -->
            <bm-row
                v-show="showFolders"
                class="position-lg-static position-absolute d-lg-block px-0 
                h-100 col-12 col-lg-2 z-index-200 overlay top-0 bottom-0"
            >
                <bm-col cols="10" lg="12" class="mail-folder-tree-wrapper bg-surface h-100">
                    <div class="h-100 scroller scroller-visible-on-hover">
                        <mail-folder-tree class="d-inline-block " @toggle-folders="toggleFolders" />
                    </div>
                </bm-col>
            </bm-row>
            <bm-col cols="12" lg="3" class="pl-lg-2 px-0 d-lg-block" :class="hideListInResponsiveMode ? 'd-none' : ''">
                <mail-message-list class="h-100" />
            </bm-col>
            <bm-col lg="7" class="overflow-auto">
                <router-view />
            </bm-col>
        </bm-row>
        <bm-button
            variant="primary"
            class="d-lg-none position-absolute bottom-1 right-1 z-index-110"
            :class="hideListInResponsiveMode ? 'd-none' : 'd-block'"
            @click="composeNewMessage"
        >
            <bm-icon icon="pencil" />
        </bm-button>
    </div>
</template>

<script>
import { BmFormCheckbox, BmLabelIcon, BmButton, BmCol, BmIcon, BmRow, MakeUniq } from "@bluemind/styleguide";
import { mapState } from "vuex";
import favicon from "../assets/favicon.png";
import injector from "@bluemind/inject";
import MailAppL10N from "@bluemind/webapp.mail.l10n";
import MailFolderTree from "./MailFolder/MailFolderTree";
import MailMessageList from "./MailMessageList/MailMessageList";
import MailToolbar from "./MailToolbar/";
import MailSearchForm from "./MailSearchForm";
import MessagesOptionsForMobile from "./MessagesOptionsForMobile";

export default {
    name: "MailApp",
    components: {
        BmFormCheckbox,
        BmButton,
        BmCol,
        BmLabelIcon,
        BmIcon,
        BmRow,
        MailFolderTree,
        MailMessageList,
        MailSearchForm,
        MailToolbar,
        MessagesOptionsForMobile
    },

    mixins: [MakeUniq],
    componentI18N: { messages: MailAppL10N },
    data() {
        return {
            userSession: injector.getProvider("UserSession").get(),
            showFolders: false,
            darkened: false
        };
    },
    computed: {
        ...mapState("mail-webapp", ["selectedMessageKeys", "messageFilter"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        isMessageComposerDisplayed() {
            const routePath = this.$route.path;
            return (
                routePath.endsWith("new") ||
                routePath.endsWith("reply") ||
                routePath.endsWith("replyAll") ||
                routePath.endsWith("forward")
            );
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
        setFavicon();
        document.title = this.$t("mail.application.title") + " - Bluemind";
    },
    methods: {
        composeNewMessage() {
            this.$router.navigate("mail:new");
        },
        toggleFolders() {
            this.showFolders = !this.showFolders;
        },
        switchWebmail() {
            injector
                .getProvider("UserSettingsPersistence")
                .get()
                .setOne(this.userSession.userId, "mail-application", '"webmail"')
                .then(() => location.replace("/webmail/"));
        }
    }
};

function setFavicon() {
    const link = document.querySelector("link[rel*='icon']") || document.createElement("link");
    link.type = "image/x-icon";
    link.rel = "shortcut icon";
    link.href = favicon;
    document.getElementsByTagName("head")[0].appendChild(link);
}
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.flex-fill {
    min-height: 0;
}

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
    .mail-folder-tree {
        min-width: 100%;
    }
    .mail-folder-tree-wrapper {
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
}
</style>
