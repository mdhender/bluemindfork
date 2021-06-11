export default {
    generate(prefix = "xxxxxxxx") {
        return (prefix + "-xxxx-4xxx-yxxx-xxxxxxxxxxxx")
            .replace(/[xy]/g, c => {
                const r = (Math.random() * 16) | 0,
                    v = c === "x" ? r : (r & 0x3) | 0x8;
                return v.toString(16);
            })
            .toUpperCase();
    },
    isUUID(uid) {
        const regex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
        return regex.test(uid);
    }
};
