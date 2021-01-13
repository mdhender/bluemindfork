<template>
    <div class="preferences position-absolute w-100 h-100 overlay d-flex" @click="TOGGLE_PREFERENCES()">
        <global-events @keydown.esc="TOGGLE_PREFERENCES()" />
        <div
            v-if="status === 'loading'"
            class="position-absolute h-100 w-100 d-flex align-items-center z-index-200 text-center overlay"
            @click.stop
        >
            <bm-spinner class="flex-fill" :size="2.5" />
        </div>
        <bm-container fluid class="flex-fill bg-surface m-lg-5" @click.stop>
            <bm-row class="h-100">
                <pref-left-panel :user="user" :sections="SECTIONS" :class="selectedSection ? 'd-none' : ''" />
                <pref-right-panel
                    :class="selectedSection ? 'd-flex' : 'd-none'"
                    :sections="SECTIONS"
                    :status="status"
                    @close="TOGGLE_PREFERENCES()"
                    @changeStatus="newStatus => (status = newStatus)"
                    @save="save"
                />
            </bm-row>
        </bm-container>
    </div>
</template>

<script>
import GlobalEvents from "vue-global-events";
import SettingsL10N from "../../../l10n/settings/";
import { BmContainer, BmRow, BmSpinner } from "@bluemind/styleguide";
import PrefLeftPanel from "./PrefLeftPanel";
import PrefRightPanel from "./PrefRightPanel";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import listStyleCompact from "../../../assets/list-style-compact.png";
import listStyleFull from "../../../assets/list-style-full.png";
import listStyleNormal from "../../../assets/list-style-normal.png";
import threadSettingImageOn from "../../../assets/setting-thread-on.svg";
import threadSettingImageOff from "../../../assets/setting-thread-off.svg";

export default {
    name: "Preferences",
    components: {
        GlobalEvents,
        PrefRightPanel,
        BmContainer,
        BmRow,
        PrefLeftPanel,
        BmSpinner
    },
    props: {
        applications: {
            required: true,
            type: Array
        },
        user: {
            required: true,
            type: Object
        }
    },
    componentI18N: { messages: SettingsL10N },
    data() {
        return {
            status: "loading"
        };
    },
    computed: {
        ...mapState("preferences", { selectedSection: "selectedSectionCode" }),
        ...mapGetters("preferences", ["SECTIONS"]),
        ...mapGetters("session", ["SETTINGS_CHANGED"])
    },
    watch: {
        SETTINGS_CHANGED() {
            this.status = "idle";
        }
    },
    created() {
        this.status = "idle";

        // @see https://bootstrap-vue.org/docs/directives/scrollspy#events
        this.$root.$on("bv::scrollspy::activate", sectionId => {
            this.SET_SELECTED_SECTION(parseSectionId(sectionId).sectionCode);
        });

        /**
         * Here is the 'heart' of the Settings.
         * Each section, like "mail", holds several categories, like "main". Each category holds fields.
         * These fields are created using Dynamic Components (see PrefContent).
         */
        this.SET_SECTIONS([
            {
                name: this.$t("common.general"),
                href: "/",
                icon: { name: "preferences" },
                code: "main",
                categories: [
                    {
                        code: "main",
                        name: this.$t("common.general"),
                        icon: "wrench",
                        fields: [
                            {
                                name: "Param One",
                                setting: "fake",
                                component: { template: "<div>Blabla blabla</div>" },
                                options: {}
                            },
                            {
                                name: "Param Two",
                                setting: "fake",
                                component: { template: "<div>Blabla blabla</div>" },
                                options: {}
                            },
                            {
                                name: "Param Three",
                                setting: "fake",
                                component: { template: "<div>Blabla blabla</div>" },
                                options: {}
                            },
                            {
                                name: "Param Four",
                                setting: "fake",
                                component: { template: "<div>Blabla blabla</div>" },
                                options: {}
                            }
                        ]
                    },
                    {
                        code: "advanced",
                        name: "Avancé",
                        icon: "plus",
                        fields: [
                            {
                                name: "Param A",
                                setting: "fake",
                                component: { template: "<div>Blabla blabla</div>" },
                                options: {}
                            },
                            {
                                name: "Param B",
                                setting: "fake",
                                component: { template: "<div>Blabla blabla</div>" },
                                options: {}
                            },
                            {
                                name: "Param C",
                                setting: "fake",
                                component: { template: "<div>Blabla blabla</div>" },
                                options: {}
                            },
                            {
                                name: "Param D",
                                setting: "fake",
                                component: { template: "<div>Blabla blabla</div>" },
                                options: {}
                            }
                        ]
                    }
                ]
            },
            ...this.applications
                .filter(app => app.href === "/mail/")
                .map(a => {
                    return {
                        ...a,
                        code: a.href.replaceAll("/", ""),
                        categories: [
                            {
                                code: "main",
                                name: this.$t("common.general"),
                                icon: "wrench",
                                fields: [
                                    {
                                        component: "PrefFieldChoice",
                                        name: this.$t("preferences.mail.thread"),
                                        setting: "mail_thread",
                                        options: {
                                            choices: [
                                                {
                                                    name: this.$t("preferences.mail.thread.enable"),
                                                    value: "true",
                                                    svg: threadSettingImageOn
                                                },
                                                {
                                                    name: this.$t("preferences.mail.thread.disable"),
                                                    value: "false",
                                                    svg: threadSettingImageOff
                                                }
                                            ]
                                        },
                                        availableSoon: true
                                    },
                                    {
                                        component: "PrefFieldChoice",
                                        name: this.$t("preferences.mail.message.list.display"),
                                        setting: "mail_message_list_style",
                                        options: {
                                            choices: [
                                                {
                                                    name: this.$t("preferences.mail.message.list.display.full"),
                                                    value: "full",
                                                    img: listStyleFull
                                                },
                                                {
                                                    name: this.$t("preferences.mail.message.list.display.normal"),
                                                    value: "normal",
                                                    img: listStyleNormal
                                                },
                                                {
                                                    name: this.$t("preferences.mail.message.list.display.compact"),
                                                    value: "compact",
                                                    img: listStyleCompact
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        component: "PrefFieldCheck",
                                        name: this.$t("preferences.mail.signature"),
                                        setting: "insert_signature",
                                        options: {
                                            label: this.$t("preferences.mail.signature.insert")
                                        }
                                    }
                                ]
                            }
                        ]
                    };
                })
        ]);
    },
    mounted() {
        this.SET_SELECTED_SECTION("main");
    },
    methods: {
        ...mapActions("session", ["SAVE_SETTINGS", "ROLLBACK_SETTINGS"]),
        ...mapMutations("preferences", ["TOGGLE_PREFERENCES", "SET_SELECTED_SECTION", "SET_SECTIONS"]),
        async save() {
            this.status = "loading";
            try {
                await this.SAVE_SETTINGS();
                this.status = "saved";
            } catch {
                await this.ROLLBACK_SETTINGS();
                this.status = "error";
            }
        }
    }
};

function parseSectionId(sectionId) {
    const splitSectionId = sectionId.split("-");
    return { sectionCode: splitSectionId[1], categoryCode: splitSectionId[2] };
}
</script>

<style lang="scss">
.preferences {
    z-index: 500;
}
</style>
