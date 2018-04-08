package pickaxes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener {
	private final String pickaxeFormat = getConfig().getString("name-format");

	private final String spick = getConfig().getString("spick");
	private final String xpick = getConfig().getString("xpick");
	private final String bpick = getConfig().getString("bpick");

	private final String[] blockHierarchy = getConfig().getString("block-values").split(" ");

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getPlayer() == null || event.getPlayer().getInventory().getItemInMainHand() == null)
			return;
		ItemStack mainHand = event.getPlayer().getInventory().getItemInMainHand();
		if (mainHand.getItemMeta() == null || mainHand.getItemMeta().getLore() == null)
			return;
		List<String> lore = mainHand.getItemMeta().getLore();
		if (lore.size() >= 1) {
			String pickaxe = lore.get(0);
			if (pickaxe.equals(String.format(pickaxeFormat, spick))) {
				ItemStack item = event.getBlock().getDrops().stream().findAny().orElse(null);
				if (item == null)
					return;

				Iterator<Recipe> recipes = Bukkit.getServer().recipeIterator();

				done: while (recipes.hasNext()) {
					Recipe rec = recipes.next();
					if (rec instanceof FurnaceRecipe) {
						FurnaceRecipe frec = (FurnaceRecipe) rec;
						if (frec.getInput().getType().equals(item.getType())) {
							int amount = item.getAmount();
							item = frec.getResult();
							item.setAmount(amount);

							event.getBlock().getWorld().spawn(event.getBlock().getLocation(), ExperienceOrb.class)
									.setExperience(2);
							;
							break done;
						}
					}
				}
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
				event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
			} else if (pickaxe.equals(String.format(pickaxeFormat, xpick))) {
				event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(),
						getConfig().getInt("explosion-radius"));
			} else if (pickaxe.equals(String.format(pickaxeFormat, bpick))) {
				Set<String> blocks = new HashSet<>();
				int searchRadius = getConfig().getInt("block-search-radius");
				System.out.println(searchRadius);
				for (int i = -searchRadius; i <= searchRadius; i++) {
					for (int j = -searchRadius; j <= searchRadius; j++) {
						for (int z = -searchRadius; z <= searchRadius; z++) {
							Location l = event.getBlock().getLocation();
							l.add(new Vector(i, j, z));
							blocks.add(l.getWorld().getBlockAt(l).getDrops().stream().findAny()
									.orElse(new ItemStack(Material.AIR)).getType().toString());
						}
					}
				}
				for (String s : blockHierarchy) {
					if (blocks.contains(s)) {
						int drops = (int) event.getBlock().getDrops().stream().mapToDouble(n -> n.getAmount()).sum();
						event.setCancelled(true);
						event.getBlock().setType(Material.AIR);
						event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
								new ItemStack(Material.getMaterial(s), drops));
						return;
					}
				}
				for (ItemStack items : event.getBlock().getDrops()) {
					event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), items);
				}
				event.setCancelled(true);
				event.getBlock().setType(Material.AIR);
			}
		}

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equals("spick")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				if (player.hasPermission("specialpicks.admin")) {
					String pickName;
					if (args.length == 0) {
						player.sendMessage(String.format("Please use /spick [%s | %s | %s]", spick, xpick, bpick));
						return true;
					} else {
						pickName = args[0];
					}
					if (Arrays.asList(xpick, spick, bpick).indexOf(pickName) == -1) {
						player.sendMessage(
								String.format("Invalid pickaxe type. Please use %s, %s, or %s", spick, xpick, bpick));
						return true;
					}
					ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE, 1);
					pickaxe.addEnchantment(Enchantment.DURABILITY, 1);
					ItemMeta meta = pickaxe.getItemMeta();
					meta.setLore(Arrays.asList(String.format(pickaxeFormat, pickName)));
					pickaxe.setItemMeta(meta);

					player.getInventory().addItem(pickaxe);
				}
			}
		}
		return true;

	}

	@Override
	public void onDisable() {

	}

	@Override
	public void onEnable() {
		System.out.println(getConfig().getString("name-format"));
		getConfig().options().copyDefaults(true);
		saveConfig();

		Bukkit.getPluginManager().registerEvents(this, this);
		ConsoleCommandSender log = getServer().getConsoleSender();
		log.sendMessage(ChatColor.RED + "Special Pickaxes by gamesterrex.");
		log.sendMessage(ChatColor.RED + "Contact me at <robertchen@live.com>");
	}

}
