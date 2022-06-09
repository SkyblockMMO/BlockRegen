package nl.aurorion.blockregen.system.region;

import com.google.common.base.Strings;
import lombok.extern.java.Log;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.system.region.struct.RawRegion;
import nl.aurorion.blockregen.system.region.struct.RegenerationRegion;
import nl.aurorion.blockregen.system.region.struct.RegionSelection;
import nl.aurorion.blockregen.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Log
public class RegionManager {

    private final BlockRegen plugin;

    private final Map<String, RegenerationRegion> loadedRegions = new HashMap<>();

    // Set of regions that failed to load.
    private final Set<RawRegion> failedRegions = new HashSet<>();

    private final Map<UUID, RegionSelection> selections = new HashMap<>();

    public RegionManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    // ---- Selection

    public boolean isSelecting(@NotNull Player player) {
        return selections.containsKey(player.getUniqueId());
    }

    public RegionSelection getSelection(@NotNull Player player) {
        return selections.get(player.getUniqueId());
    }

    @NotNull
    public RegionSelection getOrCreateSelection(@NotNull Player player) {
        RegionSelection selection = selections.get(player.getUniqueId());

        if (selection == null) {
            selection = new RegionSelection();

            selections.put(player.getUniqueId(), selection);
        }

        return selection;
    }

    public RegenerationRegion createRegion(@NotNull String name, RegionSelection selection) {
        Location first = selection.getFirst();
        Location second = selection.getSecond();

        if (first.getWorld() != second.getWorld()) {
            throw new IllegalStateException("Selection points have to be in the same world.");
        }

        // Find out min and max.

        Location min = new Location(first.getWorld(), Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
        Location max = new Location(first.getWorld(), Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));

        return new RegenerationRegion(name, min, max);
    }

    public boolean finishSelection(@NotNull String name, @NotNull RegionSelection selection) {
        RegenerationRegion region = createRegion(name, selection);

        addRegion(region);

        return true;
    }

    public void reattemptLoad() {
        if (failedRegions.isEmpty()) {
            return;
        }

        log.info("Reattempting to load regions...");
        int count = failedRegions.size();
        failedRegions.removeIf(rawRegion -> rawRegion.isReattempt() && loadRegion(rawRegion));
        log.info("Loaded " + (count - failedRegions.size()) + " of failed regions.");
    }

    public void load() {
        this.loadedRegions.clear();
        plugin.getFiles().getRegions().load();

        FileConfiguration regions = plugin.getFiles().getRegions().getFileConfiguration();

        ConfigurationSection section = regions.getConfigurationSection("Regions");

        if (section != null) {
            for (String name : section.getKeys(false)) {

                String minString = section.getString(name + ".Min");
                String maxString = section.getString(name + ".Max");

                RawRegion rawRegion = new RawRegion(name, minString, maxString);

                if (Strings.isNullOrEmpty(minString) || Strings.isNullOrEmpty(maxString)) {
                    this.failedRegions.add(rawRegion);
                    log.severe("Could not load region " + name + ", invalid location strings.");
                    continue;
                }

                if (!LocationUtil.isLocationLoaded(minString) || !LocationUtil.isLocationLoaded(maxString)) {
                    rawRegion.setReattempt(true);
                    this.failedRegions.add(rawRegion);
                    log.info("World for region " + name + " is not loaded. Reattempting after complete server load.");
                    continue;
                }

                loadRegion(rawRegion);
            }
        }

        log.info("Loaded " + this.loadedRegions.size() + " region(s)...");
    }

    private boolean loadRegion(RawRegion rawRegion) {
        RegenerationRegion region = rawRegion.build();

        if (region == null) {
            log.warning("Could not load region " + rawRegion.getName() + ", world " + rawRegion.getMax() + " still not loaded.");
            return false;
        }

        this.loadedRegions.put(rawRegion.getName(), region);
        log.fine("Loaded region " + rawRegion.getName());
        return true;
    }

    public void save() {
        FileConfiguration regions = plugin.getFiles().getRegions().getFileConfiguration();

        regions.set("Regions", null);

        ConfigurationSection section = ensureRegionsSection(regions);

        for (RawRegion rawRegion : new HashSet<>(this.failedRegions)) {
            ConfigurationSection regionSection = section.createSection(rawRegion.getName());

            regionSection.set("Min", rawRegion.getMin());
            regionSection.set("Max", rawRegion.getMax());
        }

        for (RegenerationRegion regenerationRegion : new HashSet<>(this.loadedRegions.values())) {
            ConfigurationSection regionSection = section.createSection(regenerationRegion.getName());

            regionSection.set("Min", LocationUtil.locationToString(regenerationRegion.getMin()));
            regionSection.set("Max", LocationUtil.locationToString(regenerationRegion.getMax()));
        }
        plugin.getFiles().getRegions().save();

        log.fine("Saved " + (this.loadedRegions.size() + this.failedRegions.size()) + " region(s)...");
    }

    private ConfigurationSection ensureRegionsSection(FileConfiguration configuration) {
        return configuration.contains("Regions") ? configuration.getConfigurationSection("Regions") : configuration.createSection("Regions");
    }

    public boolean exists(String name) {
        return this.loadedRegions.containsKey(name);
    }

    public RegenerationRegion getRegion(String name) {
        return this.loadedRegions.get(name);
    }

    public void removeRegion(String name) {
        this.loadedRegions.remove(name);
    }

    @Nullable
    public RegenerationRegion getRegion(@Nullable Location location) {
        if (location == null) return null;

        for (RegenerationRegion region : this.loadedRegions.values()) {
            if (region.contains(location))
                return region;
        }
        return null;
    }

    public void addRegion(@NotNull RegenerationRegion region) {
        this.loadedRegions.put(region.getName(), region);
        log.fine("Added region " + region.getName());
        save();
    }

    public Map<String, RegenerationRegion> getLoadedRegions() {
        return Collections.unmodifiableMap(loadedRegions);
    }
}