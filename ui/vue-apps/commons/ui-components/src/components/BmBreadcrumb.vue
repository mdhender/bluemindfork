<script>
import { BBreadcrumb } from "bootstrap-vue";
export default {
    name: "BmBreadcrumb",
    extends: BBreadcrumb
};
</script>

<style lang="scss">
@import "../css/_variables";

.breadcrumb {
    align-items: center;
    background-color: $surface !important;
    margin: 0 !important;
    padding: $sp-3 $sp-4 !important;
}
</style>
