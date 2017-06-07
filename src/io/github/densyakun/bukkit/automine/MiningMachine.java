package io.github.densyakun.bukkit.automine;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MiningMachine {
	private static final int TICKS = 20;

	public static Material BLOCK_MATERIAL = Material.IRON_BLOCK;
	public static Sound SOUND_SWITCH = Sound.CLICK;
	public static Sound SOUND_REFUEL = Sound.WOOD_CLICK;
	public static Sound SOUND_MINING = Sound.STEP_GRASS;

	public static Material[] TOOLS = new Material[] { Material.WOOD_PICKAXE, Material.WOOD_SPADE,
			Material.STONE_PICKAXE, Material.STONE_SPADE, Material.IRON_PICKAXE, Material.IRON_SPADE,
			Material.GOLD_PICKAXE, Material.GOLD_SPADE, Material.DIAMOND_PICKAXE, Material.DIAMOND_SPADE };
	public static int MINING_BLOCKS = 16;

	World world;
	int x;
	int y;
	int z;
	Block block;
	Location loc;
	boolean enable = false;
	int power = 0;
	ItemStack tool;
	Direction direction = Direction.zero;
	int tick = 0;

	public MiningMachine(Block block) {
		this(block.getWorld(), block.getX(), block.getY(), block.getZ());
	}

	public MiningMachine(World world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		block = world.getBlockAt(x, y, z);
		loc = block.getLocation();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Block ? equals((Block) obj) : super.equals(obj);
	}

	public boolean equals(Block obj) {
		return equals(obj.getWorld(), obj.getX(), obj.getY(), obj.getZ());
	}

	public boolean equals(World world, int x, int y, int z) {
		return this.world.getUID().equals(world.getUID()) && this.x == x && this.y == y && this.z == z;
	}

	public World getWorld() {
		return world;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public Block getBlock() {
		return block;
	}

	public Location getLocation() {
		return loc;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		if (this.enable = enable) {
			if (tool == null) {
				Chest c = getChest();
				if (c != null) {
					Inventory i = c.getBlockInventory();
					for (int a = 0; a < TOOLS.length; a++) {
						int b = i.first(TOOLS[a]);
						if (b != -1) {
							ItemStack item = new ItemStack(i.getItem(b));
							i.remove(item);
							setTool(item);
						}
					}
				}
			}
		}
		world.playSound(loc, SOUND_SWITCH, 1, enable ? 12 : 0);
	}

	public void tick() {
		if (power == 0 || tool == null || block.getType() != BLOCK_MATERIAL) {
			setEnable(false);
			return;
		}
		--power;
		world.playSound(loc, SOUND_MINING, 1, 12);

		if (TICKS < ++tick) {
			tick = 0;

			boolean v = true;
			for (int w = 1; w <= MINING_BLOCKS; w++) {
				Block b = null;
				switch (direction) {
				case left:
					b  = world.getBlockAt(x - w, y, z);
					break;
				case right:
					b  = world.getBlockAt(x + w, y, z);
					break;
				case down:
					b  = world.getBlockAt(x, y - w, z);
					break;
				case up:
					b  = world.getBlockAt(x, y + w, z);
					break;
				case back:
					b  = world.getBlockAt(x, y, z - w);
					break;
				case forward:
					b  = world.getBlockAt(x, y, z + w);
					break;
				default:
					broken();
					w = MINING_BLOCKS;
					break;
				}
				if (b != null && b.getType() != Material.AIR) {
					try {
						@SuppressWarnings("unchecked")
						ItemStack[] d = ((Collection<ItemStack>) b.getClass().getMethod("getDrops", ItemStack.class)
								.invoke(b, tool)).toArray(new ItemStack[0]);
						Chest c = getChest();
						if (c == null) {
							for (int e = 0; e < d.length; e++) {
								world.dropItem(loc, d[e]);
							}
						} else {
							Inventory i = c.getBlockInventory();
							HashMap<Integer, ItemStack> e = i.addItem(d);
							Iterator<Integer> f = e.keySet().iterator();
							while (f.hasNext()) {
								world.dropItem(loc, e.get(f.next()));
							}
						}

						b.setType(Material.AIR);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
							| NoSuchMethodException | SecurityException e) {
						e.printStackTrace();
					}

					//TODO 道具の耐久値を減らす

					v = false;
					break;
				}
			}

			if (v) {
				setEnable(false);
			}
		}
	}

	public int getPower() {
		return power;
	}

	public void refuel(int fuelTime) {
		power += fuelTime;
		world.playSound(loc, SOUND_REFUEL, 1, 0);
	}

	public void setTool(ItemStack tool) {
		if (this.tool != null) {
			Chest c = getChest();
			if (c == null || c.getBlockInventory().addItem(this.tool).keySet().size() != 0)
				world.dropItem(loc, this.tool);
		}
		this.tool = tool;
		world.playSound(loc, SOUND_REFUEL, 1, 12);
	}

	public ItemStack getTool() {
		return tool;
	}

	public void broken() {
		if (this.tool != null) {
			Chest c = getChest();
			if (c == null || c.getBlockInventory().addItem(this.tool).keySet().size() != 0)
				world.dropItem(loc, this.tool);
		}
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public Direction getDirection() {
		return direction;
	}

	public Chest getChest() {
		BlockState b = world.getBlockAt(x - 1, y, z).getState();
		if (!(b instanceof Chest)) {
			b = world.getBlockAt(x + 1, y, z).getState();
			if (!(b instanceof Chest)) {
				b = world.getBlockAt(x, y - 1, z).getState();
				if (!(b instanceof Chest)) {
					b = world.getBlockAt(x, y + 1, z).getState();
					if (!(b instanceof Chest)) {
						b = world.getBlockAt(x, y, z - 1).getState();
						if (!(b instanceof Chest)) {
							b = world.getBlockAt(x, y, z + 1).getState();
						}
					}
				}
			}
		}
		if (b instanceof Chest)
			return (Chest) b;
		return null;
	}
}
