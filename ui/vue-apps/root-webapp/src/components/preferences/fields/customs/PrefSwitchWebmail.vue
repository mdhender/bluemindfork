<template>
    <div class="d-flex pref-switch-webmail align-items-center text-neutral">
        <img :src="image" alt="" />
        <div class="text-wrapper">
            <strong class="font-italic">{{ warning }}</strong>
            <ul class="font-italic">
                <li v-for="(feature, index) in features" :key="index">
                    <em>{{ feature }}</em>
                </li>
            </ul>
            <bm-read-more href="https://doc.bluemind.net/release/5.1/category/la-messagerie" />
        </div>
    </div>
</template>

<script>
import { BmReadMore } from "@bluemind/ui-components";
import { BaseField } from "@bluemind/preferences";
import mailAppVersionSettingImageClassic from "~/../assets/setting-mail-app-version-classic.png";
import mailAppVersionSettingImageModern from "~/../assets/setting-mail-app-version-modern.png";

export default {
    name: "PrefSwitchWebmail",
    components: { BmReadMore },
    mixins: [BaseField],
    computed: {
        image() {
            return this.disabled ? mailAppVersionSettingImageClassic : mailAppVersionSettingImageModern;
        },
        warning() {
            return this.$t(`preferences.mail.advanced.switch.${this.disabled ? "classic" : "modern"}.missing_features`);
        },
        features() {
            return [
                this.$t("preferences.mail.advanced.switch.features.filehosting"),
                this.$t("preferences.mail.advanced.switch.features.move_folder")
            ];
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-switch-webmail {
    flex-direction: column;
    gap: $sp-4;
    @include from-lg {
        flex-direction: row;
        gap: $sp-8;
    }

    img {
        width: 246px;
        max-width: 50%;
    }
    .text-wrapper {
        @include from-lg {
            padding-right: $sp-8;
        }
    }
}
</style>
