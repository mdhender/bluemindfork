<template>
    <div :id="categoryId(section.code, category.code)" class="pref-category">
        <div v-for="field in category.fields" :key="field.name" class="pref-field">
            <h2 class="py-4">
                {{ field.name }}
                <span v-if="field.availableSoon" class="available-soon">{{ $t("common.available_soon") }}</span>
            </h2>
            <bm-form-group :aria-label="field.name" :disabled="field.availableSoon">
                <component
                    :is="field.component"
                    v-bind="{
                        'local-user-settings': localUserSettings,
                        setting: field.setting,
                        name: field.name,
                        options: field.options
                    }"
                />
                <template v-if="field.options && field.options.additional_component">
                    <component :is="field.options.additional_component" />
                </template>
            </bm-form-group>
        </div>
    </div>
</template>

<script>
import PrefFieldCheck from "./fields/PrefFieldCheck";
import PrefFieldChoice from "./fields/PrefFieldChoice";
import PrefFieldSelect from "./fields/PrefFieldSelect";
import PrefAlwaysShowQuota from "./fields/customs/PrefAlwaysShowQuota";
import PrefRemoteImage from "./fields/customs/PrefRemoteImage";
import PrefResetLocalData from "./fields/customs/PrefResetLocalData";
import PrefMixin from "./mixins/PrefMixin";

import { BmFormGroup } from "@bluemind/styleguide";

export default {
    name: "PrefCategory",
    components: {
        BmFormGroup,
        PrefFieldCheck,
        PrefFieldChoice,
        PrefFieldSelect,
        PrefAlwaysShowQuota,
        PrefRemoteImage,
        PrefResetLocalData
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
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-category {
    .pref-field {
        padding-left: 4rem;
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
}
</style>
