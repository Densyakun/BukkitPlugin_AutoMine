package io.github.densyakun.bukkit.automine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Main extends JavaPlugin implements Listener, Runnable {

	public static String MCVERSION;
	public static String PREFIX;
	public static String KEY_MSG_MINING_SWITCH = "msg_mining_switch";
	public static String MSG_ARGUMENT_RESULT = "%RESULT%";

	public String msg_mining_switch = "";
	public List<MiningMachine> machines = new ArrayList<MiningMachine>();

	Thread thread = null;

	@Override
	public void onLoad() {
		MCVERSION = getServer().getClass().getName().split("\\.")[3];
		PREFIX = ChatColor.YELLOW + "[" + getName() + "] ";
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();
		FileConfiguration config = getConfig();
		msg_mining_switch = config.getString(KEY_MSG_MINING_SWITCH, msg_mining_switch);
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		machines.clear();
	}

	@EventHandler
	public void PlayerInteract(PlayerInteractEvent e) {
		if (e.getClickedBlock() == null || !e.getPlayer().isSneaking())
			return;

		if (e.getClickedBlock().getType() == MiningMachine.BLOCK_MATERIAL) {
			MiningMachine m = getMachine(e.getClickedBlock());
			if (m == null)
				machines.add(m = new MiningMachine(e.getClickedBlock()));

			boolean r = true;

			ItemStack itemInHand = e.getPlayer().getItemInHand();
			if (itemInHand != null && itemInHand.getType() != Material.AIR) {
				for (int a = 0; a < MiningMachine.TOOLS.length; a++) {
					if (itemInHand.getType() == MiningMachine.TOOLS[a]) {
						ItemStack i = new ItemStack(itemInHand);
						e.getPlayer().getInventory().setItemInHand(null);
						m.setTool(i);
						r = false;
						break;
					}
				}

				if (r) {
					try {
						Object a = e.getPlayer().getClass().getMethod("getHandle", new Class<?>[0])
								.invoke(e.getPlayer(), new Object[0]);
						Object b = a.getClass().getMethod("bA", new Class<?>[0]).invoke(a, new Object[0]);
						Method fuelTimeMethod = Class
								.forName("net.minecraft.server." + MCVERSION + ".TileEntityFurnace").getMethod(
										"fuelTime", Class.forName("net.minecraft.server." + MCVERSION + ".ItemStack"));
						int fuelTime = (int) fuelTimeMethod.invoke(null, b);
						if (0 < fuelTime) {
							int c = itemInHand.getAmount() - 1;
							if (c == 0) {
								e.getPlayer().getInventory().setItemInHand(null);
							} else {
								itemInHand.setAmount(c);
							}

							m.refuel(fuelTime);
							r = false;
						}
					} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException
							| IllegalArgumentException | InvocationTargetException e1) {
						e1.printStackTrace();
					}
				}
			}
			if (r) {
				boolean result = !m.isEnable();
				m.setEnable(result);
				if (result) {
					Vector v = e.getPlayer().getLocation().getDirection();
					Direction d = Direction.getDirection(v.getX(), v.getY(), v.getZ());
					m.setDirection(d);

					e.getPlayer().sendMessage(PREFIX + "Direction: " + d);
					e.getPlayer().sendMessage(PREFIX + "Power: " + m.power);
					e.getPlayer().sendMessage(PREFIX + "Tool: " + m.tool);
				}
				e.getPlayer().sendMessage(
						PREFIX + msg_mining_switch.replaceFirst(MSG_ARGUMENT_RESULT, result ? "ON" : "OFF"));
			}

			if (thread != null && thread.isAlive())
				return;
			(thread = new Thread(this)).start();
		}
	}

	@EventHandler
	public void BlockBreak(BlockBreakEvent e) {
		removeMachine(e.getBlock());
	}

	// TODO リロード等で止まらないようにする。読み書き。
	@Override
	public void run() {
		while (machines.size() > 0) {
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run() {
					for (int a = 0; a < machines.size();) {
						MiningMachine m = machines.get(a);
						if (m.enable) {
							m.tick();
						}
						if (m.block.getType() != MiningMachine.BLOCK_MATERIAL || m.power == 0 && m.tool == null) {
							m.broken();
							machines.remove(a);
						} else {
							a++;
						}
					}
				}
			});

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean contains(Block block) {
		for (int a = 0; a < machines.size(); a++) {
			if (machines.get(a).equals(block)) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(World world, int x, int y, int z) {
		for (int a = 0; a < machines.size(); a++) {
			if (machines.get(a).equals(world, x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public MiningMachine getMachine(Block block) {
		return getMachine(block.getWorld(), block.getX(), block.getY(), block.getZ());
	}

	public MiningMachine getMachine(World world, int x, int y, int z) {
		for (int a = 0; a < machines.size(); a++) {
			MiningMachine m = machines.get(a);
			if (m.equals(world, x, y, z)) {
				return m;
			}
		}
		return null;
	}

	public void removeMachine(Block block) {
		removeMachine(block.getWorld(), block.getX(), block.getY(), block.getZ());
	}

	public void removeMachine(World world, int x, int y, int z) {
		for (int a = 0; a < machines.size(); a++) {
			MiningMachine m = machines.get(a);
			if (m.equals(world, x, y, z)) {
				m.broken();
				machines.remove(a);
				break;
			}
		}
	}
}
