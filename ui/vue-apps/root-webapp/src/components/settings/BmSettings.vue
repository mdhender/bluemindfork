<template>
    <div class="bm-settings position-absolute w-100 h-100 overlay d-flex" @click="TOGGLE_SETTINGS()">
        <div
            v-if="status === 'loading'"
            class="position-absolute h-100 w-100 d-flex align-items-center z-index-200 text-center overlay"
            @click.stop
        >
            <bm-spinner class="flex-fill" :size="2.5" />
        </div>
        <bm-container fluid class="flex-fill bg-white m-lg-5" @click.stop>
            <bm-row class="h-100">
                <bm-settings-left-sidebar
                    :user="user"
                    :available-apps="availableApps"
                    :selected-app="selectedApp"
                    :class="selectedApp ? 'd-none' : ''"
                    @change="selectedApp = $event"
                    @close="TOGGLE_SETTINGS()"
                />
                <bm-settings-content
                    :class="selectedApp ? 'd-flex' : 'd-none'"
                    :selected-app="selectedApp"
                    :available-apps="availableApps"
                    :status="status"
                    :applications="applications"
                    @close="TOGGLE_SETTINGS()"
                    @change="selectedApp = $event"
                    @changeStatus="newStatus => (status = newStatus)"
                    @save="save"
                />
            </bm-row>
        </bm-container>
    </div>
</template>

<script>
import SettingsL10N from "../../../l10n/settings/";
import { BmContainer, BmRow, BmSpinner } from "@bluemind/styleguide";
import BmSettingsLeftSidebar from "./BmSettingsLeftSidebar";
import BmSettingsContent from "./BmSettingsContent";
import { mapActions, mapMutations } from "vuex";

export default {
    name: "BmSettings",
    components: {
        BmSettingsContent,
        BmContainer,
        BmRow,
        BmSettingsLeftSidebar,
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
            selectedApp: null,
            status: "loading"
        };
    },
    computed: {
        availableApps() {
            return this.applications.filter(app => app.href === "/mail/");
        }
    },
    created() {
        this.status = "idle";
    },
    methods: {
        ...mapActions("session", ["UPDATE_ALL_SETTINGS"]),
        ...mapMutations("root-app", ["TOGGLE_SETTINGS"]),
        async save(userSettings) {
            this.status = "loading";
            try {
                await this.UPDATE_ALL_SETTINGS(userSettings);
                this.status = "saved";
            } catch {
                this.status = "error";
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.bm-settings {
    z-index: 500;
}
</style>
