<script>
import BmSkeleton from "./BmSkeleton";

export default {
    name: "BmSkeletonAvatar",
    extends: BmSkeleton,
    props: {
        type: {
            required: false,
            type: String,
            default: "avatar"
        }
    }
};
</script>
