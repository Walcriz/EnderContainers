package fr.utarwyn.endercontainers.menu;

import fr.utarwyn.endercontainers.TestHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AbstractMenuTest {

    @Mock
    private AbstractMenu menu;

    @BeforeClass
    public static void setUpClass() {
        TestHelper.setUpServer();
    }

    @Before
    public void setUp() {
        doCallRealMethod().when(this.menu).getInventory();
        doCallRealMethod().when(this.menu).reloadInventory();
        doCallRealMethod().when(this.menu).open(any(Player.class));
        doCallRealMethod().when(this.menu).getFilledSlotsNb();
        doCallRealMethod().when(this.menu).getMapContents();
    }

    @Test
    public void inventory() {
        assertThat(this.menu.inventory).isNull();
        this.menu.inventory = mock(Inventory.class);
        assertThat(this.menu.getInventory()).isNotNull().isEqualTo(this.menu.inventory);
    }

    @Test
    public void reloadInventory() {
        Inventory inventory = mock(Inventory.class);

        int rows = 5;
        String title = "very long default inventory title";

        when(Bukkit.getServer().createInventory(
                any(InventoryHolder.class), any(Integer.class), any(String.class)
        )).thenReturn(inventory);

        when(this.menu.getRows()).thenReturn(rows);
        when(this.menu.getTitle()).thenReturn(title);

        // Create a new inventory
        this.menu.reloadInventory();

        assertThat(this.menu.inventory).isNotNull();
        verify(this.menu, times(1)).prepare();
        verify(Bukkit.getServer(), times(1)).createInventory(this.menu, rows * 9, title.substring(0, 32));

        // Reload an inventory with itemstacks
        ItemStack[] itemList = this.getFakeItemList();

        when(this.menu.inventory.getContents()).thenReturn(itemList);
        this.menu.reloadInventory();
        verify(this.menu.inventory, times(1)).setContents(itemList);
    }

    @Test
    public void open() {
        Player player = mock(Player.class);
        Inventory inventory = mock(Inventory.class);

        when(Bukkit.getServer().isPrimaryThread()).thenReturn(true);

        this.menu.inventory = inventory;
        this.menu.open(player);

        verify(player, times(1)).openInventory(inventory);
    }

    @Test
    public void filledSlotsNb() {
        this.menu.inventory = mock(Inventory.class);

        when(this.menu.inventory.getContents()).thenReturn(this.getFakeItemList());
        assertThat(this.menu.getFilledSlotsNb()).isEqualTo(3);
    }

    @Test
    public void mapContents() {
        this.menu.inventory = mock(Inventory.class);
        when(this.menu.inventory.getContents()).thenReturn(this.getFakeItemList());

        Map<Integer, ItemStack> map = this.menu.getMapContents();

        assertThat(map).isNotNull().hasSize(3);
        assertThat(map.get(0)).isNull();
        assertThat(map.get(2)).isNotNull();
        assertThat(map.get(8)).isNotNull();
    }

    /**
     * Create a fake item list to put into mocked inventories.
     *
     * @return item list with a size of 10, but with 3 real itemstacks.
     */
    private ItemStack[] getFakeItemList() {
        ItemStack[] itemList = new ItemStack[10];
        itemList[2] = new ItemStack(Material.ENDER_CHEST);
        itemList[8] = new ItemStack(Material.ENDER_CHEST);
        itemList[9] = new ItemStack(Material.ENDER_CHEST);
        return itemList;
    }

}
