<template>
    <bm-col lg="10" cols="12" class="bm-settings-content d-lg-flex flex-column h-100">
        <div class="d-lg-none d-block bm-settings-content-header py-2 px-3">
            <bm-button variant="inline-light" class="d-lg-none btn-sm mr-auto" @click="$emit('change', null)">
                <bm-icon icon="arrow-back" size="2x" />
            </bm-button>
            <h2 class="d-inline text-white align-middle">{{ displayedApp.name }}</h2>
        </div>
        <bm-button-close class="align-self-end d-lg-block d-none mt-3 mx-3" @click="$emit('close')" />
        <div class="bm-settings-navbar">
            <bm-app-icon :icon-app="displayedApp.icon" class="px-4 d-inline-block pt-2" />
            <h2 class="border-bottom border-primary d-inline-block text-primary py-2">
                <bm-label-icon icon="wrench" icon-size="lg"> {{ $t("common.general") }} </bm-label-icon>
            </h2>
        </div>
        <div class="border-bottom border-secondary" />
        <div class="pl-5 py-4 overflow-auto">
            <h2 class="pb-4">{{ $t("settings.mail.thread") }}</h2>
            <bm-form-group :aria-label="$t('settings.mail.thread')">
                <bm-form-radio-group v-model="localUserSettings.mail_thread" class="d-flex flex-wrap">
                    <bm-form-radio value="true" class="ml-5" :aria-label="$t('settings.mail.thread.enable')">
                        <!-- eslint-disable-next-line vue/no-v-html -->
                        <template #img><div v-html="threadSettingImageOn" /></template>
                        <template>
                            {{ $t("settings.mail.thread.enable") }}
                        </template>
                    </bm-form-radio>
                    <bm-form-radio value="false" class="ml-5" :aria-label="$t('settings.mail.thread.disable')">
                        <!-- eslint-disable-next-line vue/no-v-html -->
                        <template #img><div v-html="threadSettingImageOff" /></template>
                        <template> {{ $t("settings.mail.thread.disable") }}</template>
                    </bm-form-radio>
                </bm-form-radio-group>
            </bm-form-group>
            <h2 class="py-4">{{ $t("settings.mail.message.list.display") }}</h2>
            <bm-form-group :aria-label="$t('settings.mail.message.list.display')">
                <bm-form-radio-group v-model="localUserSettings.mail_message_list_style" class="d-flex flex-wrap">
                    <bm-form-radio
                        class="ml-5"
                        value="full"
                        :aria-label="$t('settings.mail.message.list.display.full')"
                    >
                        <!-- eslint-disable-next-line vue/no-v-html -->
                        <template #img><img :src="listStyleFull" alt="null" /></template>
                        <template>
                            {{ $t("settings.mail.message.list.display.full") }}
                        </template>
                    </bm-form-radio>
                    <bm-form-radio
                        class="ml-5"
                        value="normal"
                        :aria-label="$t('settings.mail.message.list.display.normal')"
                    >
                        <!-- eslint-disable-next-line vue/no-v-html -->
                        <template #img><img :src="listStyleNormal" alt="null" /></template>
                        <template>
                            {{ $t("settings.mail.message.list.display.normal") }}
                        </template>
                    </bm-form-radio>
                    <bm-form-radio
                        class="ml-5"
                        value="compact"
                        :aria-label="$t('settings.mail.message.list.display.compact')"
                    >
                        <!-- eslint-disable-next-line vue/no-v-html -->
                        <template #img><img :src="listStyleCompact" alt="null" /></template>
                        <template>
                            {{ $t("settings.mail.message.list.display.compact") }}
                        </template>
                    </bm-form-radio>
                </bm-form-radio-group>
            </bm-form-group>
        </div>
        <div class="d-flex mt-auto pl-5 py-3 border-top border-secondary">
            <bm-button
                type="submit"
                variant="primary"
                :disabled="!hasChanged"
                @click.prevent="$emit('save', localUserSettings)"
            >
                {{ $t("common.save") }}
            </bm-button>
            <bm-button type="reset" variant="simple-dark" class="ml-3" :disabled="!hasChanged" @click.prevent="cancel">
                {{ $t("common.cancel") }}
            </bm-button>
            <div v-if="status === 'error'" class="ml-5 text-danger d-flex align-items-center font-weight-bold">
                <bm-icon icon="exclamation-circle" class="mr-1" /> {{ $t("settings.save.error") }}
            </div>
            <div v-if="status === 'saved'" class="ml-5 text-success d-flex align-items-center font-weight-bold">
                <bm-icon icon="exclamation-circle" class="mr-1" /> {{ $t("settings.save.success") }}
            </div>
        </div>
    </bm-col>
</template>

<script>
import listStyleCompact from "../../../assets/list-style-compact.png";
import listStyleFull from "../../../assets/list-style-full.png";
import listStyleNormal from "../../../assets/list-style-normal.png";
import threadSettingImageOn from "../../../assets/setting-thread-on.svg";
import threadSettingImageOff from "../../../assets/setting-thread-off.svg";
import {
    BmButton,
    BmButtonClose,
    BmCol,
    BmIcon,
    BmLabelIcon,
    BmFormGroup,
    BmFormRadio,
    BmFormRadioGroup
} from "@bluemind/styleguide";
import BmAppIcon from "../BmAppIcon";
import { mapState } from "vuex";

export default {
    name: "BmSettings",
    components: {
        BmButton,
        BmButtonClose,
        BmCol,
        BmIcon,
        BmLabelIcon,
        BmAppIcon,
        BmFormRadio,
        BmFormRadioGroup,
        BmFormGroup
    },
    props: {
        selectedApp: {
            type: Object,
            default: null
        },
        status: {
            type: String,
            required: true
        },
        applications: {
            required: true,
            type: Array
        }
    },
    data() {
        return {
            displayedApp: this.selectedApp || this.applications.find(app => app.href === "/mail/"),
            threadSettingImageOn,
            threadSettingImageOff,
            listStyleNormal,
            listStyleFull,
            listStyleCompact,
            localUserSettings: {}
        };
    },
    computed: {
        ...mapState("session", ["userSettings"]),
        hasChanged() {
            return JSON.stringify(this.localUserSettings) !== JSON.stringify(this.userSettings);
        }
    },
    watch: {
        userSettings() {
            this.initUserSettings();
        },
        hasChanged() {
            this.$emit("changeStatus", "idle");
        }
    },
    mounted() {
        this.initUserSettings();
        const selector = document.querySelector(".bm-settings-navbar .bm-app-icon svg");
        if (selector) {
            selector.setAttribute("height", 21);
            selector.setAttribute("width", 30);
        }
    },
    methods: {
        cancel() {
            this.initUserSettings();
        },
        initUserSettings() {
            this.localUserSettings = { ...this.userSettings };
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.bm-settings-content {
    .bm-settings-content-header {
        background-color: $info-dark;
    }
    .bm-settings-navbar > * {
        border-bottom-width: 3px !important;
    }
    .bm-settings-navbar .bm-app-icon svg {
        height: 2rem;
    }
}
</style>
