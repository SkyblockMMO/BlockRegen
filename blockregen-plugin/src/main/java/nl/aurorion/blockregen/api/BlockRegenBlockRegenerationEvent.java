package nl.aurorion.blockregen.api;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.system.regeneration.struct.RegenerationProcess;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired before a block is regenerated.
 * Cancelling this event causes the block not to regenerate into another, also deletes the regeneration process.
 */
public class BlockRegenBlockRegenerationEvent extends BlockRegenBlockEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @Getter
    @Setter
    private boolean cancelled = false;

    /**
     * Regeneration process responsible for this block.
     */
    @Getter
    private final RegenerationProcess regenerationProcess;

    public BlockRegenBlockRegenerationEvent(RegenerationProcess regenerationProcess) {
        super(regenerationProcess.getBlock(), regenerationProcess.getPreset());
        this.regenerationProcess = regenerationProcess;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }


    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }


    /*
     * Shortcuts.
     * */

    public XMaterial getRegenerateInto() {
        return regenerationProcess.getRegenerateInto();
    }

    public void setRegenerateInto(XMaterial material) {
        regenerationProcess.setRegenerateInto(material);
    }
}