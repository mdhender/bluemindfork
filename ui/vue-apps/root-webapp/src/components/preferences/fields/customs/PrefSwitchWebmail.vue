<template>
    <div v-if="!collapsed" class="d-flex pref-switch-webmail align-items-center text-neutral">
        <img :src="image" alt="" />
        <div class="text-wrapper">
            <strong class="font-italic">{{ warning }}</strong>
            <ul class="font-italic">
                <li v-for="(feature, index) in features" :key="index">
                    <em>{{ feature }}</em>
                </li>
            </ul>
            <a target="_blank" href="https://doc.bluemind.net/release/5.0/category/la-messagerie">
                {{ $t("common.read_more") }}
            </a>
        </div>
    </div>
</template>

<script>
import BaseField from "../../mixins/BaseField";
import mailAppVersionSettingImageClassic from "~/../assets/setting-mail-app-version-classic.png";
import mailAppVersionSettingImageModern from "~/../assets/setting-mail-app-version-modern.png";

export default {
    name: "PrefSwitchWebmail",
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
@import "~@bluemind/styleguide/css/mixins/_responsiveness";
@import "~@bluemind/styleguide/css/_variables";

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
