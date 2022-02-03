<template>
    <div v-if="!collapsed" class="d-flex pref-switch-webmail align-items-center text-secondary">
        <img class="mr-5" :src="image" alt="" />
        <div>
            <strong class="font-italic">{{ warning }}</strong>
            <ul class="font-italic">
                <li v-for="(feature, index) in features" :key="index">
                    <em>{{ feature }}</em>
                </li>
            </ul>
            <a target="_blank" href="https://forge.bluemind.net/confluence/display/BM4/Messagerie+BlueMind">
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
