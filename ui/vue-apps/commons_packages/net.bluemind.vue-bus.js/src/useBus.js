import { getCurrentInstance } from "vue";

export function useBus() {
    const vm = getCurrentInstance();
    return vm.proxy.$bus;
}
