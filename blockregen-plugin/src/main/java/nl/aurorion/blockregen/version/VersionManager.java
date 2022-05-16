package nl.aurorion.blockregen.version;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import lombok.Getter;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.util.ParseUtil;
import nl.aurorion.blockregen.version.ancient.AncientMethods;
import nl.aurorion.blockregen.version.api.Methods;
import nl.aurorion.blockregen.version.api.WorldEditProvider;
import nl.aurorion.blockregen.version.api.WorldGuardProvider;
import nl.aurorion.blockregen.version.current.LatestMethods;
import nl.aurorion.blockregen.version.current.LatestWorldEditProvider;
import nl.aurorion.blockregen.version.current.LatestWorldGuardProvider;
import nl.aurorion.blockregen.version.legacy.LegacyMethods;
import nl.aurorion.blockregen.version.legacy.LegacyWorldEditProvider;
import nl.aurorion.blockregen.version.legacy.LegacyWorldGuardProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionManager {

    private final BlockRegen plugin;

    @Getter
    private final String version = loadNMSVersion();

    private WorldEditPlugin worldEdit;
    private WorldGuardPlugin worldGuard;

    @Getter
    private WorldEditProvider worldEditProvider;
    @Getter
    private WorldGuardProvider worldGuardProvider;
    @Getter
    private Methods methods;

    public VersionManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    public void load() {

        setupWorldEdit();
        setupWorldGuard();

        /*
         * Latest - 1.13+
         * Legacy - 1.12 - 1.9
         * Ancient - 1.8 - 1.7
         */
        switch (version) {
            // Try to catch 1.7 into ancient. Might work on some occasions.
            case "v1_7":
            case "v1_8":
                useWorldEdit(LegacyWorldEditProvider::new);
                useWorldGuard(LegacyWorldGuardProvider::new);
                this.methods = new AncientMethods();
                break;
            case "v1_9":
            case "v1_10":
            case "v1_11":
            case "v1_12":
                useWorldEdit(LegacyWorldEditProvider::new);
                useWorldGuard(LegacyWorldGuardProvider::new);
                this.methods = new LegacyMethods();
                break;
            case "v1_13":
            case "v1_14":
            case "v1_15":
            case "v1_16":
            case "v1_17":
            case "v1_18":
            default:
                useWorldEdit(LatestWorldEditProvider::new);
                useWorldGuard(LatestWorldGuardProvider::new);
                this.methods = new LatestMethods();
                break;
        }
    }

    public interface InstanceProvider<X, Y> {
        X provide(Y plugin);
    }

    public void useWorldGuard(InstanceProvider<WorldGuardProvider, WorldGuardPlugin> instanceProvider) {
        if (worldGuardProvider == null && this.worldGuard != null) {
            this.worldGuardProvider = instanceProvider.provide(worldGuard);
        }
    }

    public void useWorldEdit(InstanceProvider<WorldEditProvider, WorldEditPlugin> instanceProvider) {
        if (worldEditProvider == null && this.worldEdit != null) {
            this.worldEditProvider = instanceProvider.provide(worldEdit);
        }
    }

    public String loadNMSVersion() {
        Pattern pattern = Pattern.compile("v\\d+_\\d+");

        Matcher matcher = pattern.matcher(Bukkit.getServer().getClass().getPackage().getName());
        return matcher.find() ? matcher.group() : null;
    }

    // Simple version number to compare, v1_8 -> 18
    private int composeVersionNumber(String versionString) {
        String num = versionString.replace("v", "")
                .replace("_", "")
                .replace(".", "");
        return ParseUtil.parseInteger(num);
    }

    public boolean isAbove(String versionString, boolean include) {
        int version = composeVersionNumber(versionString);
        int current = composeVersionNumber(this.version);
        return include ? current >= version : current > version;
    }

    public boolean isBelow(String versionString, boolean include) {
        int version = composeVersionNumber(versionString);
        int current = composeVersionNumber(this.version);
        return include ? current <= version : current < version;
    }

    private void setWorldEditProvider(WorldEditProvider worldEditProvider) {
        this.worldEditProvider = worldEditProvider;
    }

    private void setWorldGuardProvider(WorldGuardProvider worldGuardProvider) {
        this.worldGuardProvider = worldGuardProvider;
    }

    private void setupWorldEdit() {

        if (worldEditProvider != null)
            return;

        Plugin worldEditPlugin = plugin.getServer().getPluginManager().getPlugin("WorldEdit");

        if (!(worldEditPlugin instanceof WorldEditPlugin))
            return;

        this.worldEdit = (WorldEditPlugin) worldEditPlugin;
        plugin.getConsoleOutput().info("WorldEdit found! &aEnabling regions.");
    }

    private void setupWorldGuard() {
        if (worldGuardProvider != null)
            return;

        Plugin worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");

        if (!(worldGuardPlugin instanceof WorldGuardPlugin))
            return;

        this.worldGuard = (WorldGuardPlugin) worldGuardPlugin;
        plugin.getConsoleOutput().info("WorldGuard found! &aSupporting it's Region protection.");
    }
}
