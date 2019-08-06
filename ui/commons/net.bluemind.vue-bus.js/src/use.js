export function use(plugin) {
    const installedPlugins = this._installedPlugins || (this._installedPlugins = []);
    if (installedPlugins.indexOf(plugin) > -1) {
        return this;
    }

    // additional parameters
    const args = Array.prototype.slice.call(arguments, 1);
    args.unshift(this);
    if (typeof plugin.install === "function") {
        plugin.install.apply(plugin, args);
    } else if (typeof plugin === "function") {
        plugin.apply(null, args);
    }
    installedPlugins.push(plugin);
    return this;
}
