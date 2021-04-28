<template>
    <div :id="categoryId(section.code, category.code)" class="pref-category">
        <div v-for="group in filteredFieldGroups" :key="group.title" class="pref-field">
            <h2 class="pt-4 pb-2">
                {{ group.title }}
                <span v-if="group.availableSoon" class="available-soon">{{ $t("common.available_soon") }}</span>
            </h2>
            <bm-form-group :aria-label="group.title" :disabled="group.availableSoon">
                <template v-for="field in group.fields">
                    <bm-form-group :key="field.name" :aria-label="field.name" :label="field.name">
                        <component
                            :is="field.component"
                            v-bind="{
                                'local-user-settings': localUserSettings,
                                setting: field.setting,
                                name: field.name,
                                options: field.options
                            }"
                        />
                    </bm-form-group>
                    <template v-if="field.options && field.options.additional_component">
                        <component :is="field.options.additional_component" :key="field.name" />
                    </template>
                </template>
            </bm-form-group>
        </div>
    </div>
</template>

<script>
import PrefFieldCheck from "./fields/PrefFieldCheck";
import PrefFieldChoice from "./fields/PrefFieldChoice";
import PrefFieldComboBox from "./fields/PrefFieldComboBox";
import PrefFieldSelect from "./fields/PrefFieldSelect";
import PrefAlwaysShowQuota from "./fields/customs/PrefAlwaysShowQuota";
import PrefEnableNotifications from "./fields/customs/PrefEnableNotifications";
import PrefIMSetPhonePresence from "./fields/customs/PrefIMSetPhonePresence";
import PrefManageIdentities from "./fields/customs/PrefManageIdentities";
import PrefPassword from "./fields/customs/PrefPassword";
import PrefRemoteImage from "./fields/customs/PrefRemoteImage";
import PrefResetLocalData from "./fields/customs/PrefResetLocalData";
import PrefWorksHours from "./fields/customs/PrefWorksHours";
import PrefMixin from "./mixins/PrefMixin";

import { BmFormGroup } from "@bluemind/styleguide";

export default {
    name: "PrefCategory",
    components: {
        BmFormGroup,
        PrefFieldCheck,
        PrefFieldChoice,
        PrefFieldComboBox,
        PrefFieldSelect,
        PrefAlwaysShowQuota,
        PrefEnableNotifications,
        PrefIMSetPhonePresence,
        PrefManageIdentities,
        PrefPassword,
        PrefRemoteImage,
        PrefResetLocalData,
        PrefWorksHours
    },
    mixins: [PrefMixin],
    props: {
        section: {
            type: Object,
            required: true
        },
        category: {
            type: Object,
            required: true
        },
        localUserSettings: {
            type: Object,
            required: true
        }
    },
    computed: {
        filteredFieldGroups() {
            return this.category.groups.filter(
                group => !Object.prototype.hasOwnProperty.call(group, "condition") || group.condition
            );
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-category {
    .pref-field {
        padding-left: 4rem;
        padding-right: 4rem;
        border-bottom: 1px solid $light;
    }
    .available-soon {
        color: white;
        background-color: $yellow;
        &::before,
        &::after {
            content: "\00a0";
        }
    }

    .pref-field-combobox,
    .bm-form-timepicker,
    .bm-form-select,
    .bm-form-input {
        width: 24rem !important;
    }
}
</style>
