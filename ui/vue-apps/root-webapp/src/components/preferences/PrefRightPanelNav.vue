<template>
    <bm-list-group v-bm-scrollspy:scroll-area class="pref-right-panel-nav" horizontal>
        <pref-section-navbar v-for="section in sections" :ref="section.code" :key="section.code" :section="section" />
    </bm-list-group>
</template>

<script>
import { BmListGroup, BmScrollspy } from "@bluemind/styleguide";
import PrefMixin from "./mixins/PrefMixin";
import PrefSectionNavbar from "./PrefSectionNavbar";
import { mapState } from "vuex";

export default {
    name: "PrefRightPanelNav",
    components: {
        BmListGroup,
        PrefSectionNavbar
    },
    directives: { BmScrollspy },
    mixins: [PrefMixin],
    props: {
        sections: {
            required: true,
            type: Array
        }
    },
    computed: {
        ...mapState("preferences", ["offset"])
    },
    watch: {
        offset() {
            this.moveSectionsNavbar();
        }
    },
    mounted() {
        this.moveSectionsNavbar();
    },
    methods: {
        moveSectionsNavbar() {
            this.sections.forEach(section => {
                const navbar = this.$refs[section.code][0].$el;
                const anchor = document.getElementById("section-" + section.code);
                if (anchor) {
                    const translate = Math.max(Math.min(this.offset - anchor.offsetTop, navbar.offsetHeight), 0);
                    navbar.style.opacity = Math.min(1, translate / navbar.offsetHeight);
                    navbar.style.transform = "translateY(" + -translate + "px)";
                } else {
                    navbar.style.transform = "translateY(-" + navbar.offsetHeight + "px)";
                }
            });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-right-panel-nav {
    min-height: 4em;
    position: relative;
    .pref-section-navbar {
        position: absolute;
        top: 4em;
    }
}
</style>
