package me.StevenLawson.TotalFreedomMod.Listener;

import java.util.Iterator;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import me.StevenLawson.TotalFreedomMod.TFM_LandmineData;
import me.StevenLawson.TotalFreedomMod.TFM_UserInfo;
import me.StevenLawson.TotalFreedomMod.TFM_Util;
import me.StevenLawson.TotalFreedomMod.TotalFreedomMod;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class TFM_PlayerListener implements Listener
{
    private TotalFreedomMod plugin;
    private static final Logger log = Logger.getLogger("Minecraft");

    public TFM_PlayerListener(TotalFreedomMod instance)
    {
        this.plugin = instance;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        Player player = event.getPlayer();
        Action action = event.getAction();
        Material material = event.getMaterial();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
        {
            if (material == Material.WATER_BUCKET)
            {
                player.getInventory().setItemInHand(new ItemStack(Material.COOKIE, 1));
                player.sendMessage(ChatColor.GOLD + "Does this look like a waterpark to you?");
                event.setCancelled(true);
                return;
            }
            else if (material == Material.LAVA_BUCKET)
            {
                player.getInventory().setItemInHand(new ItemStack(Material.COOKIE, 1));
                player.sendMessage(ChatColor.GOLD + "LAVA NO FUN, YOU EAT COOKIE INSTEAD, NO?");
                event.setCancelled(true);
                return;
            }
        }
        else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
        {
            if (material == Material.STICK)
            {
                TFM_UserInfo playerdata = TFM_UserInfo.getPlayerData(player);
                if (playerdata.mobThrowerEnabled())
                {
                    Location player_pos = player.getLocation();
                    Vector direction = player_pos.getDirection().normalize();

                    if (!LivingEntity.class.isAssignableFrom(playerdata.mobThrowerCreature().getEntityClass()))
                    {
                        return;
                    }

                    LivingEntity rezzed_mob = (LivingEntity) player.getWorld().spawn(player_pos.add(direction.multiply(2.0)), playerdata.mobThrowerCreature().getEntityClass());
                    rezzed_mob.setVelocity(direction.multiply(playerdata.mobThrowerSpeed()));
                    playerdata.enqueueMob(rezzed_mob);

                    event.setCancelled(true);
                }
            }
            else if (material == Material.GUNPOWDER)
            {
                TFM_UserInfo playerdata = TFM_UserInfo.getPlayerData(player);

                if (playerdata.isMP44Armed())
                {
                    if (playerdata.toggleMP44Firing())
                    {
                        playerdata.startArrowShooter(plugin);
                    }
                    else
                    {
                        playerdata.stopArrowShooter();
                    }

                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event)
    {
        Player p = event.getPlayer();
        TFM_UserInfo playerdata = TFM_UserInfo.getPlayerData(p);

        boolean do_freeze = false;
        if (TotalFreedomMod.allPlayersFrozen)
        {
            if (!TFM_Util.isUserSuperadmin(p, plugin))
            {
                do_freeze = true;
            }
        }
        else
        {
            if (playerdata.isFrozen())
            {
                do_freeze = true;
            }
        }

        if (do_freeze)
        {
            Location from = event.getFrom();
            Location to = event.getTo().clone();

            to.setX(from.getX());
            to.setY(from.getY());
            to.setZ(from.getZ());

            event.setTo(to);
        }

        if (playerdata.isCaged())
        {
            Location target_pos = p.getLocation().add(0, 1, 0);

            boolean out_of_cage = false;
            if (!target_pos.getWorld().equals(playerdata.getCagePos().getWorld()))
            {
                out_of_cage = true;
            }
            else
            {
                out_of_cage = target_pos.distance(playerdata.getCagePos()) > 2.5;
            }

            if (out_of_cage)
            {
                playerdata.setCaged(true, target_pos, playerdata.getCageMaterial(TFM_UserInfo.CageLayer.INNER), playerdata.getCageMaterial(TFM_UserInfo.CageLayer.OUTER));
                playerdata.regenerateHistory();
                playerdata.clearHistory();
                TFM_Util.buildHistory(target_pos, 2, playerdata);
                TFM_Util.generateCube(target_pos, 2, playerdata.getCageMaterial(TFM_UserInfo.CageLayer.OUTER));
                TFM_Util.generateCube(target_pos, 1, playerdata.getCageMaterial(TFM_UserInfo.CageLayer.INNER));
            }
        }

        if (playerdata.isOrbiting())
        {
            if (p.getVelocity().length() < playerdata.orbitStrength() * (2.0 / 3.0))
            {
                p.setVelocity(new Vector(0, playerdata.orbitStrength(), 0));
            }
        }
        
        if (TotalFreedomMod.landminesEnabled && TotalFreedomMod.allowExplosions)
        {
            Iterator<TFM_LandmineData> landmines = TFM_LandmineData.landmines.iterator();
            while (landmines.hasNext())
            {
                TFM_LandmineData landmine = landmines.next();
                
                Location landmine_pos = landmine.landmine_pos;
                if (landmine_pos.getBlock().getType() != Material.TNT)
                {
                    landmines.remove();
                    continue;
                }

                if (!landmine.player.equals(p))
                {
                    if (p.getWorld().equals(landmine_pos.getWorld()))
                    {
                        if (p.getLocation().distance(landmine_pos) <= landmine.radius)
                        {
                            landmine.landmine_pos.getBlock().setType(Material.AIR);
                            
                            TNTPrimed tnt1 = landmine_pos.getWorld().spawn(landmine_pos, TNTPrimed.class);
                            tnt1.setFuseTicks(40);
                            tnt1.setPassenger(p);
                            tnt1.setVelocity(new Vector(0.0, 2.0, 0.0));
                            
                            TNTPrimed tnt2 = landmine_pos.getWorld().spawn(p.getLocation(), TNTPrimed.class);
                            tnt2.setFuseTicks(1);
                            
                            p.setGameMode(GameMode.SURVIVAL);
                            landmines.remove();
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event)
    {
        Player p = event.getPlayer();

        TFM_UserInfo playerdata = TFM_UserInfo.getPlayerData(p);
        playerdata.incrementMsgCount();

        if (playerdata.getMsgCount() > 10)
        {
            TFM_Util.bcastMsg(p.getName() + " was automatically kicked for spamming chat.", ChatColor.RED);
            TFM_Util.autoEject(p, "No Spamming");
            playerdata.resetMsgCount();

            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        String command = event.getMessage();
        Player p = event.getPlayer();
        
        TFM_UserInfo playerdata = TFM_UserInfo.getPlayerData(p);
        playerdata.incrementMsgCount();
        
        if (playerdata.getMsgCount() > 10)
        {
            TFM_Util.bcastMsg(p.getName() + " was automatically kicked for spamming commands.", ChatColor.RED);
            TFM_Util.autoEject(p, "No Spamming");
            playerdata.resetMsgCount();
            
            TFM_Util.wipeDropEntities(true);

            event.setCancelled(true);
            return;
        }

        if (TotalFreedomMod.preprocessLogEnabled)
        {
            log.info(String.format("[PREPROCESS_COMMAND] %s(%s): %s", p.getName(), ChatColor.stripColor(p.getDisplayName()), command));
        }

        command = command.toLowerCase().trim();

        boolean block_command = false;

        if (Pattern.compile("^/stop").matcher(command).find())
        {
            if (!TFM_Util.isUserSuperadmin(p, plugin))
            {
                block_command = true;
            }
        }
        else if (Pattern.compile("^/reload").matcher(command).find())
        {
            if (!TFM_Util.isUserSuperadmin(p, plugin))
            {
                block_command = true;
            }
        }
        else if (Pattern.compile("^/save-").matcher(command).find())
        {
            if (!TFM_Util.isUserSuperadmin(p, plugin))
            {
                block_command = true;
            }
        }
        
        if (block_command)
        {
            TFM_Util.autoEject(p, "That command is prohibited.");
            TFM_Util.bcastMsg(p.getName() + " was automatically kicked for using harmful commands.", ChatColor.RED);
        }
        else
        {
            if (Pattern.compile("^/time").matcher(command).find())
            {
                p.sendMessage(ChatColor.GRAY + "Server-side time changing is disabled. Please use /ptime to set your own personal time.");
                block_command = true;
            }
        }

        if (block_command)
        {
            p.sendMessage(ChatColor.RED + "That command is prohibited.");
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        if (TotalFreedomMod.autoEntityWipe)
        {
            if (event.getPlayer().getWorld().getEntities().size() > 750)
            {
                event.setCancelled(true);
            }
            else
            {
                event.getItemDrop().remove();
            }
        }
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event)
    {
        TFM_UserInfo playerdata = TFM_UserInfo.getPlayerData(event.getPlayer());
        playerdata.disarmMP44();
        if (playerdata.isCaged())
        {
            playerdata.regenerateHistory();
            playerdata.clearHistory();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        TFM_UserInfo playerdata = TFM_UserInfo.getPlayerData(event.getPlayer());
        playerdata.disarmMP44();
        if (playerdata.isCaged())
        {
            playerdata.regenerateHistory();
            playerdata.clearHistory();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        try
        {
            if (!Bukkit.getOnlineMode())
            {
                Player p = event.getPlayer();
                if (TotalFreedomMod.superadmins.contains(p.getName().toLowerCase()))
                {
                    String user_ip = p.getAddress().getAddress().toString().replaceAll("/", "").trim();
                    if (user_ip != null && !user_ip.isEmpty())
                    {
                        TFM_Util.checkPartialSuperadminIP(user_ip, plugin);
                        
                        if (!TotalFreedomMod.superadmin_ips.contains(user_ip))
                        {
                            TFM_Util.bcastMsg(p.getName() + " might be a fake! IP: " + user_ip, ChatColor.RED);
                            p.setOp(false);
                            p.setGameMode(GameMode.SURVIVAL);
                            p.getInventory().clear();
                        }
                        else
                        {
                            //TFM_Util.bcastMsg(p.getName() + " is a verified superadmin.", ChatColor.GREEN);
                        }
                    }
                }
            }
        }
        catch (Throwable ex)
        {
        }
    }
}
