export function adapt({ key, value }) {
    return { key, value: JSON.parse(value) };
}

export function toRemote({ key, value }) {
    return { key, value: JSON.stringify(value) };
}
