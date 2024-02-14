import { getCurrentInstance } from "vue"; // getCurrentInstance is an internal function, use with caution

export default () => {
    const vm = getCurrentInstance();
    return vm.proxy.$root.$bvModal;
};
