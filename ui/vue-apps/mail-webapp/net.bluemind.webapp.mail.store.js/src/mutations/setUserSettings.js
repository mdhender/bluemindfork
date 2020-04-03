export function setUserSettings(state, settings) {
    state.userSettings = Object.assign(state.userSettings, settings);
}
