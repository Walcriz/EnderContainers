package fr.utarwyn.endercontainers.inventory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import fr.utarwyn.endercontainers.Managers;
import fr.utarwyn.endercontainers.compatibility.CompatibilityHelper;
import fr.utarwyn.endercontainers.configuration.Files;
import fr.utarwyn.endercontainers.configuration.LocaleKey;
import fr.utarwyn.endercontainers.enderchest.EnderChest;
import fr.utarwyn.endercontainers.enderchest.EnderChestManager;
import fr.utarwyn.endercontainers.util.uuid.UUIDFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents a custom enderchest with all contents.
 *
 * @author Utarwyn
 * @since 2.0.0
 */
public class EnderChestInventory extends AbstractInventoryHolder {

    /**
     * Enderchest who generated this inventory
     */
    private final EnderChest chest;

    /**
     * Internal map to cache all contents of the chest (even those not displayed in the container).
     */
    private ConcurrentMap<Integer, ItemStack> contents;

    /**
     * Constructs an inventory which contains contents of an enderchest.
     *
     * @param chest The enderchest
     */
    public EnderChestInventory(EnderChest chest) {
        this.chest = chest;
        this.itemMovingRestricted = false;

        this.reloadInventory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepare() {
        this.contents = this.chest.getContents();

        // Add all items in the container (only those which can be displayed)
        int size = this.chest.getMaxSize();
        this.contents.forEach((index, item) -> {
            if (index < size) {
                this.inventory.setItem(index, item);
            }
        });

        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.GRAY_STAINED_GLASS_PANE);
        meta.setDisplayName("");

        int toolbarStart = (getRows() - 1) * 9;
        this.inventory.setItem(toolbarStart, newBorder(meta));
        this.inventory.setItem(toolbarStart + 1, newBorder(meta));
        this.inventory.setItem(toolbarStart + 2, newBorder(meta));
        this.inventory.setItem(toolbarStart + 3, newBorder(meta));
        this.inventory.setItem(toolbarStart + 4, newBorder(meta));
        this.inventory.setItem(toolbarStart + 5, newBorder(meta));
        this.inventory.setItem(toolbarStart + 6, newBorder(meta));
        this.inventory.setItem(toolbarStart + 7, newBorder(meta));

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backItemMeta = back.getItemMeta();
        backItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cBack"));
        back.setItemMeta(backItemMeta);
        this.inventory.setItem(toolbarStart + 8, back);
    }

    private ItemStack newBorder(ItemMeta meta) {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Retrieve all contents of the chest.
     *
     * @return map with all items (even those which are out of bounds)
     */
    public ConcurrentMap<Integer, ItemStack> getContents() {
        return this.contents;
    }

    /**
     * Updates the whole content of this chest, based on its container.
     * Check first for all items in the container, but take also those which are in cache (not displayed).
     */
    public void updateContentsFromContainer() {
        Preconditions.checkNotNull(this.inventory, "container seems to be null");
        Preconditions.checkNotNull(this.contents, "internal contents map seems to be null");

        ItemStack[] containerContents = this.inventory.getContents();

        // Replace cache contents with container contents if filled
        for (int i = 0; i < containerContents.length - 9; i++) {
            if (containerContents[i] != null) {
                this.contents.put(i, containerContents[i]);
            } else {
                this.contents.remove(i);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getRows() {
        return this.chest.getRows() + 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getTitle() {
        String num = String.valueOf(this.chest.getNum() + 1);
        String playername = Objects.requireNonNull(UUIDFetcher.getName(this.chest.getOwner()));

        return Files.getLocale().getMessage(LocaleKey.MENU_CHEST_TITLE)
                .replace("%player%", playername)
                .replace("%num%", num);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(Player player) {
        // Save and delete the player context if the owner of the chest is offline
        Player owner = Bukkit.getPlayer(this.chest.getOwner());
        boolean offlineOwner = owner == null || !owner.isOnline();

        // Save chest inventory if owner is offline or forced by the configuration (experimental)
        if (offlineOwner || Files.getConfiguration().isSaveOnChestClose()) {
            Managers.get(EnderChestManager.class).savePlayerContext(this.chest.getOwner(), offlineOwner);
        }

        // Play the closing sound
        Sound sound = CompatibilityHelper.searchSound("CHEST_CLOSE", "BLOCK_CHEST_CLOSE");
        if (Files.getConfiguration().isGlobalSound()) {
            player.getWorld().playSound(player.getLocation(), sound, 1f, 1f);
        } else {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        }
    }

    @Override
    public boolean onClick(Player player, int slot) {
        int toolbarStart = (getRows() - 1) * 9;
        if (slot >= toolbarStart && slot < getRows() * 9) {

            if (slot == toolbarStart + 8) {
                player.performCommand("dm open bank-item");
            }

            return true;
        }

        return false;
    }
}
