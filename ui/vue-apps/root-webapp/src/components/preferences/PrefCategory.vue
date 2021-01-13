<template>
    <div :id="'section-' + section.code + '-' + category.code" class="pref-category">
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
            </bm-form-group>
        </div>
    </div>
</template>

<script>
import { BmFormGroup } from "@bluemind/styleguide";
import PrefFieldChoice from "./PrefFieldChoice";
import PrefFieldCheck from "./PrefFieldCheck";

export default {
    name: "PrefCategory",
    components: {
        BmFormGroup,
        PrefFieldCheck,
        PrefFieldChoice
    },
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
