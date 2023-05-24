<template>
    <div class="topbar-desktop" :class="{ 'active-search': activeSearch }">
        <div class="new">
            <new-message :template="activeFolder === MY_TEMPLATES.key" />
        </div>
        <div class="search">
            <mail-search-box @active="activeSearch = $event" />
        </div>
        <div class="toolbar h-100 w-100">
            <mail-toolbar :compact="activeSearch" />
        </div>

        <div v-if="canSwitchWebmail" class="switch pr-5">
            <bm-form-checkbox
                switch
                left-label
                checked="true"
                class="switch-webmail text-right text-secondary"
                @change="switchWebmail"
            >
                {{ $t("mail.main.switch.webmail") }}
            </bm-form-checkbox>
        </div>
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { BmFormCheckbox } from "@bluemind/ui-components";
import BmRoles from "@bluemind/roles";
import { MY_TEMPLATES } from "~/getters";
import NewMessage from "../NewMessage";
import MailSearchBox from "../MailSearch/MailSearchBox";
import MailToolbar from "../MailToolbar/MailToolbar";

export default {
    components: {
        BmFormCheckbox,
        MailSearchBox,
        MailToolbar,
        NewMessage
    },
    data() {
        return {
            userSession: inject("UserSession"),
            activeSearch: false
        };
    },
    computed: {
        ...mapState("mail", {
            activeFolder: "activeFolder"
        }),
        ...mapGetters("mail", {
            MY_TEMPLATES
        }),
        canSwitchWebmail() {
            return (
                this.userSession &&
                this.userSession.roles.includes(BmRoles.HAS_MAIL_WEBAPP) &&
                this.userSession.roles.includes(BmRoles.HAS_WEBMAIL)
            );
        }
    },
    methods: {
        async switchWebmail() {
            await inject("UserSettingsPersistence").setOne(this.userSession.userId, "mail-application", '"webmail"');
            location.replace("/webmail/");
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/variables";
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";
@import "~@bluemind/ui-components/src/css/_type";
.topbar-desktop {
    display: flex;
    flex: 1 1 auto;
    align-items: center;

    .new {
        padding: 0 $sp-5;
        display: flex;
        justify-content: center;
        width: 16.7%;
    }

    .toolbar {
        flex: 0 1;
        order: 2;
        width: 100%;
        @include until-lg {
            display: none;
        }
    }
    & > .search {
        width: 25%;
    }

    &.active-search {
        & > .search {
            flex-grow: 2;
            max-width: 80%;
            .mail-search-box-context {
                width: calc(16.67vw - 3.5rem);
            }
        }
        & > .new {
            height: base-px-to-rem(30);
            width: 3.5rem !important;
            .slot-wrapper {
                display: none;
            }
        }
        & > .switch {
            display: none !important;
        }
    }

    & > .switch {
        flex: 1 1;
        order: 4;

        .switch-webmail label {
            @extend %caption-bold;
            max-width: $custom-switch-width * 3;
            color: $secondary-fg;
            $switch-offset: math.div(2 * $line-height-small - $custom-switch-height, 2);
            $switch-indicator-offset: $switch-offset +
                math.div($custom-switch-height - $custom-switch-indicator-size, 2);
            &::before {
                top: $switch-offset !important;
            }
            &::after {
                top: $switch-indicator-offset !important;
            }
        }
    }
}
</style>
