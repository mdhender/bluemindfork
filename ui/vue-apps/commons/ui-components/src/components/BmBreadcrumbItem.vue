<script>
import { BBreadcrumbItem } from "bootstrap-vue";
export default {
    name: "BmBreadcrumbItem",
    extends: BBreadcrumbItem
};
</script>

<style lang="scss">
@import "../css/_variables";

.breadcrumb-item {
    a {
        text-decoration: none;
        display: inline-block;
    }
    &:hover {
        text-decoration: underline;
    }
    &.active {
        font-weight: $font-weight-bold;
    }
}
</style>
