<template>
    <bm-container fluid class="flex-fill d-flex flex-column mail-app">
        <bm-row align-v="center" class="shadow-sm bg-surface py-2 py-xl-0 topbar z-index-250">
            <bm-col
                cols="2"
                md="4"
                lg="2"
                order="0" 
                class="d-lg-block"
                :class="composerOrMessageIsDisplayed ? 'd-none' : ''"
            >
                <bm-button
                    variant="link"
                    class="d-inline-block d-lg-none btn-transparent-bg" 
                    @click="toggleFolders"
                >
                    <bm-icon icon="burger-menu" size="2x" />
                </bm-button>
                <bm-button
                    variant="primary"
                    class="text-nowrap ml-3 d-lg-inline-block d-none"
                    @click="composeNewMessage"
                >
                    <bm-label-icon icon="plus">{{ $t("mail.main.new") }}</bm-label-icon>
                </bm-button>
            </bm-col>
            <bm-col
                cols="9"
                md="2"
                xl="3"
                order="1"
                class="d-lg-block"
                :class="composerOrMessageIsDisplayed ? 'd-none' : ''"
            >
                <mail-search-form />
            </bm-col>
            <bm-col
                class="d-md-inline-block d-lg-block h-100"
                cols="12"
                md="6"
                lg="7"
                order="last"
                :class="composerOrMessageIsDisplayed ? '' : 'd-none'"
            >
                <mail-toolbar class="mx-auto mx-xl-0" />
            </bm-col>
        </bm-row>
        <bm-row class="flex-fill position-relative flex-nowrap">
            <!-- v-show is overridden by d-lg-block in large devices -->
            <bm-row
                v-show="showFolders" 
                class="position-lg-static position-absolute d-lg-block px-0 
                h-100 col col-lg-2 z-index-200 overlay no-gutters top-0 bottom-0"
            >
                <bm-col cols="10" lg="12" class="mail-folder-tree-wrapper bg-surface h-100">
                    <div class="h-100 scroller scroller-visible-on-hover position-relative ">
                        <mail-folder-tree class="d-inline-block " @toggle-folders="toggleFolders" />
                    </div>
                </bm-col>
            </bm-row>
            <bm-col
                cols="12"
                md="4"
                lg="3"
                class="pl-lg-2 px-0 d-lg-block" 
                :class="composerOrMessageIsDisplayed ? 'd-none' : ''"
            >
                <mail-message-list class="h-100" />
            </bm-col>
            
            <router-view />
        </bm-row>
        <bm-button
            variant="primary"
            class="d-lg-none position-absolute bottom-1 right-1 z-index-110"
            :class="composerOrMessageIsDisplayed ? 'd-none' : 'd-block'"
            @click="composeNewMessage"
        >
            <bm-icon icon="pencil" />
        </bm-button>
        <mail-purge-modal />
        <bm-application-alert :alerts="alerts" class="z-index-250">
            <template v-slot="slotProps">
                <mail-alert-renderer :alert="slotProps.alert" />
            </template>
        </bm-application-alert>
    </bm-container>
</template>

<script>
import { BmApplicationAlert, BmLabelIcon, BmIcon, BmButton, BmCol, BmContainer, BmRow, MakeUniq } 
    from "@bluemind/styleguide";
import MailAlertRenderer from "./MailAlertRenderer";
import { mapActions, mapState } from "vuex";
import MailAppL10N from "@bluemind/webapp.mail.l10n";
import MailFolderTree from "./MailFolderTree";
import MailMessageList from "./MailMessageList/MailMessageList";
import MailPurgeModal from "./MailPurgeModal";
import MailToolbar from "./MailToolbar/";
import MailSearchForm from "./MailSearchForm";
import injector from "@bluemind/inject";

export default {
    name: "MailApp",
    components: {
        BmApplicationAlert,
        BmButton,
        BmCol,
        BmContainer,
        BmLabelIcon,
        BmIcon,
        BmRow,
        MailAlertRenderer,
        MailFolderTree,
        MailMessageList,
        MailPurgeModal,
        MailSearchForm,
        MailToolbar
    },

    mixins: [MakeUniq],
    componentI18N: { messages: MailAppL10N },
    data(){
        return {
            windowWidth: window.innerWidth,
            showFolders: this.windowWidth >= 768
        };
    },
    computed: {
        ...mapState("alert", ["alerts"]),
        ...mapState("mail-webapp", ["currentMessageKey"]),
        getWindowWidth(){
            return window.innerWidth;
        },
        isMessageComposerDisplayed() {
            const routePath = this.$route.path;
            return (
                routePath.endsWith("new") ||
                routePath.endsWith("reply") ||
                routePath.endsWith("replyAll") ||
                routePath.endsWith("forward")
            );
        },
        composerOrMessageIsDisplayed(){
            return this.isMessageComposerDisplayed || this.currentMessageKey;
        },
    },
    created: function() {
        const userSession = injector.getProvider("UserSession").get();
        this.bootstrap(userSession.login);
    },
    mounted(){
        window.onresize = () => {
            this.windowWidth = window.innerWidth;
        };
    },
    methods: {
        ...mapActions("mail-webapp", ["bootstrap"]),
        composeNewMessage() {
            this.$router.push({ name: "newMessage" });
        },
        toggleFolders (){
            this.showFolders = !this.showFolders;
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
body > div {
    display: flex;
    flex-direction: column;
}

.flex-fill {
    min-height: 0;
}

.mail-app .topbar {
    flex: 0 0 4em;

    @media (max-width: map-get($grid-breakpoints, 'lg')) {
        background-color: $info-dark;
        
        .btn-link{
            background-color: none;
            color: $light; 
        }
    }
}

.mail-app .bm-application-alert {
    position: absolute;
    bottom: $sp-1;
}

.mail-folder-tree {
    min-width: 100%;
}

.mail-folder-tree-wrapper {
     @media (max-width: map-get($grid-breakpoints, 'lg')) {
         box-shadow: $box-shadow-lg;
    }
}
</style>
