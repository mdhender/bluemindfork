<template>
    <bm-form-group :disabled="disabled" class="mb-6">
        <template #label>{{ $t("preferences.mail.compose.default_font") }}</template>
        <bm-form-font-selector
            :default-font="value"
            :disabled="disabled"
            :extra-font-families="extraFontFamilies"
            @input="setDefaultFont"
        />
    </bm-form-group>
</template>

<script>
import { BmFormGroup, BmFormFontSelector } from "@bluemind/ui-components";
import OneSettingField from "../../mixins/OneSettingField";

export default {
    name: "PrefComposerDefaultFont",
    components: {
        BmFormGroup,
        BmFormFontSelector
    },
    mixins: [OneSettingField],
    computed: {
        extraFontFamilies() {
            return this.$store.getters["settings/EXTRA_FONT_FAMILIES"];
        }
    },
    methods: {
        setDefaultFont(font) {
            this.value = font.id;
        }
    }
};
</script>
