package fr.utarwyn.endercontainers.inventory.menu;

import fr.utarwyn.endercontainers.compatibility.CompatibilityHelper;
import fr.utarwyn.endercontainers.compatibility.ServerVersion;
import fr.utarwyn.endercontainers.configuration.Files;
import fr.utarwyn.endercontainers.configuration.LocaleKey;
import fr.utarwyn.endercontainers.enderchest.EnderChest;
import fr.utarwyn.endercontainers.enderchest.context.PlayerContext;
import fr.utarwyn.endercontainers.inventory.AbstractInventoryHolder;
import fr.utarwyn.endercontainers.util.uuid.UUIDFetcher;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the menu with the list of all enderchests.
 *
 * @author Utarwyn
 * @since 2.0.0
 */
public class EnderChestListMenu extends AbstractInventoryHolder {

    /**
     * Static fields which represents the maximum number of items per page
     */
    private static final int PER_PAGE = 52;

    /**
     * Bukkit skull material
     */
    private static final Material SKULL_MATERIAL;

    /**
     * Represents the item to go to the previous page
     */
    private static final ItemStack PREV_PAGE_ITEM;

    /**
     * Represents the item to go to the next page
     */
    private static final ItemStack NEXT_PAGE_ITEM;

    static {
        if (ServerVersion.isNewerThan(ServerVersion.V1_12)) {
            SKULL_MATERIAL = Material.PLAYER_HEAD;
        } else {
            SKULL_MATERIAL = CompatibilityHelper.searchMaterial("SKULL_ITEM");
        }

        PREV_PAGE_ITEM = new ItemStack(SKULL_MATERIAL, 1, (short) 3);
        NEXT_PAGE_ITEM = new ItemStack(SKULL_MATERIAL, 1, (short) 3);

        SkullMeta prevSkullMeta = (SkullMeta) PREV_PAGE_ITEM.getItemMeta();
        prevSkullMeta.setOwner("MHF_ArrowLeft");
        prevSkullMeta.setDisplayName(ChatColor.RED
                + Files.getLocale().getMessage(LocaleKey.MENU_PREV_PAGE));
        PREV_PAGE_ITEM.setItemMeta(prevSkullMeta);

        SkullMeta nextSkullMeta = (SkullMeta) NEXT_PAGE_ITEM.getItemMeta();
        nextSkullMeta.setOwner("MHF_ArrowRight");
        nextSkullMeta.setDisplayName(ChatColor.RED
                + Files.getLocale().getMessage(LocaleKey.MENU_NEXT_PAGE));
        NEXT_PAGE_ITEM.setItemMeta(nextSkullMeta);
    }

    /**
     * The player context
     */
    private final PlayerContext context;

    /**
     * Current page for the player who has opened the inventory
     */
    private int page;

    /**
     * Constructs the list menu based on a player context.
     *
     * @param context context used for the preparation of the inventory
     */
    public EnderChestListMenu(PlayerContext context) {
        this.context = context;
        this.page = 1;

        this.reloadInventory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepare() {
        // Calculate the number of enderchests to display in the inventory
        int min = this.getFirstChestIndex();
        int max = Files.getConfiguration().getMaxEnderchests();
        int nb = Math.min(min + PER_PAGE + 2, max);

        this.inventory.clear();

        // Adding chest items
        for (int num = min; num < nb; num++) {
            Optional<EnderChest> chest = this.context.getChest(num);

            if (chest.isPresent() && (chest.get().isAccessible() || !Files.getConfiguration().isOnlyShowAccessibleEnderchests())) {
                chest.get().updateRowCount();
                this.inventory.setItem(num - min, this.getItemStackOf(chest.get()));
            }
        }

        // Adding previous page item (if the user is not on the first page)
        if (this.page > 1) {
            this.inventory.setItem(52, PREV_PAGE_ITEM);
            this.inventory.setItem(53, null);
        }

        // Adding next page item (if there is more chests than the current page can display)
        int nbForNext = min + PER_PAGE;
        if (this.page == 1) nbForNext += 2;

        if (nbForNext < max) {
            this.inventory.setItem(53, NEXT_PAGE_ITEM);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getRows() {
        int count = Math.min(PER_PAGE, Files.getConfiguration().getMaxEnderchests());
        return (int) Math.ceil(count / 9D);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getTitle() {
        String name = UUIDFetcher.getName(this.context.getOwner());
        return Files.getLocale().getMessage(LocaleKey.MENU_MAIN_TITLE)
                .replace("%player%", Objects.requireNonNull(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onClick(Player player, int slot) {
        ItemStack item = this.inventory.getItem(slot);
        if (item == null) return false;

        // Check for previous/next page
        if (item.getType() == SKULL_MATERIAL) {
            if (slot == PER_PAGE) this.page--;
            else if (slot == PER_PAGE + 1) this.page++;

            this.reloadInventory();
            this.open(player);
            return false;
        }

        // Open the selected chest
        Sound sound;
        if (this.context.openEnderchestInventory(player, this.getFirstChestIndex() + slot)) {
            sound = CompatibilityHelper.searchSound("CLICK", "UI_BUTTON_CLICK");
        } else {
            sound = CompatibilityHelper.searchSound("VILLAGER_NO", "ENTITY_VILLAGER_NO");
        }
        player.playSound(player.getLocation(), sound, 1f, 1f);

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(Player player) {
        // Not implemented
    }

    /**
     * Generate an itemstack with information of an enderchest
     * (capacity, accessibility, number, etc.)
     *
     * @param ec The enderchest processed
     * @return The itemstack generated
     */
    private ItemStack getItemStackOf(EnderChest ec) {
        DyeColor dyeColor = ec.isAccessible() ? this.getDyePercentageColor(ec.getFillPercentage()) : DyeColor.BLACK;
        int amount = Files.getConfiguration().isNumberingEnderchests() ? ec.getNum() + 1 : 1;

        ItemStack itemStack;

        if (ServerVersion.isNewerThan(ServerVersion.V1_12)) {
            itemStack = new ItemStack(CompatibilityHelper.searchMaterial(dyeColor.name() + "_STAINED_GLASS_PANE"), amount);
        } else {
            itemStack = new ItemStack(CompatibilityHelper.searchMaterial("STAINED_GLASS_PANE"), amount, dyeColor.getWoolData());
        }

        ItemMeta meta = itemStack.getItemMeta();

        List<String> lore = new ArrayList<>();

        // Update lore with the chest's status
        if (!ec.isAccessible()) {
            lore.add(Files.getLocale().getMessage(LocaleKey.MENU_CHEST_LOCKED));
        }
        if (ec.isFull()) {
            lore.add(Files.getLocale().getMessage(LocaleKey.MENU_CHEST_FULL));
        } else if (ec.isEmpty()) {
            lore.add(Files.getLocale().getMessage(LocaleKey.MENU_CHEST_EMPTY));
        }

        // Update itemstack metadata
        if (meta != null) {
            meta.setDisplayName(this.formatPaneTitle(ec,
                    Files.getLocale().getMessage(LocaleKey.MENU_PANE_TITLE)));
            meta.setLore(lore);
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /**
     * Formats the pane title from the configuration to have info about an enderchest.
     *
     * @param chest enderchest represented by the pane
     * @param title original title got from the configuration
     * @return formatted title ready to be displayed in the container
     */
    private String formatPaneTitle(EnderChest chest, String title) {
        ChatColor fillingColor = this.getPercentageColor(chest.getFillPercentage());
        ChatColor accessibilityColor = chest.isAccessible() ? ChatColor.GREEN : ChatColor.RED;

        // Adding the color before all text
        title = accessibilityColor + title;

        // Separate all placeholders to improve performance
        if (title.contains("%num%")) {
            title = title.replace("%num%", String.valueOf(chest.getNum() + 1));
        }

        if (title.contains("%counter%")) {
            title = title.replace("%counter%", fillingColor + "(" + chest.getSize() + "/" + chest.getMaxSize() + ")" + accessibilityColor);
        }

        if (title.contains("%percent%")) {
            title = title.replace("%percent%", fillingColor + "(" + String.format("%.0f", chest.getFillPercentage() * 100) + "%)" + accessibilityColor);
        }

        return title;
    }

    /**
     * Get the color in terms of a percentage between 0 and 1
     *
     * @param perc The percentage
     * @return The chat color generated
     */
    private ChatColor getPercentageColor(double perc) {
        if (perc >= 1)
            return ChatColor.DARK_RED;
        if (perc >= .5)
            return ChatColor.GOLD;

        return ChatColor.GREEN;
    }

    /**
     * Get the dye color in terms of a percentage between 0 and 1
     *
     * @param perc The percentage
     * @return The dye color used for the itemstack
     */
    private DyeColor getDyePercentageColor(double perc) {
        if (perc >= 1)
            return DyeColor.RED;
        if (perc >= .5)
            return DyeColor.ORANGE;

        return DyeColor.LIME;
    }

    /**
     * Calculate the minimum chest's number in terms of the current page
     *
     * @return The first chest's index for the current page
     */
    private int getFirstChestIndex() {
        return this.page == 1 ? 0 : Math.max(0, (this.page - 1) * PER_PAGE + 1);
    }

}
