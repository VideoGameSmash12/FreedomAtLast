package me.StevenLawson.TotalFreedomMod.Commands;

import me.StevenLawson.TotalFreedomMod.TFM_UserInfo;
import me.StevenLawson.TotalFreedomMod.TFM_Util;
import me.StevenLawson.TotalFreedomMod.TotalFreedomMod;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Command_cage extends TFM_Command
{
    @Override
    public boolean run(CommandSender sender, Player sender_p, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (senderIsConsole || TFM_Util.isUserSuperadmin(sender, plugin))
        {
            if (args.length == 0)
            {
                return false;
            }

            Player p;
            try
            {
                p = getPlayer(args[0]);
            }
            catch (CantFindPlayerException ex)
            {
                sender.sendMessage(ex.getMessage());
                return true;
            }

            TFM_UserInfo playerdata = TFM_UserInfo.getPlayerData(p);

            Material cage_material_outer = Material.GLASS;
            Material cage_material_inner = Material.AIR;
            if (args.length >= 2)
            {
                if (TFM_Util.isStopCommand(args[1]))
                {
                    playerdata.setCaged(false);
                    playerdata.regenerateHistory();
                    playerdata.clearHistory();
                    sender.sendMessage(ChatColor.GREEN + p.getName() + " uncaged.");
                    return true;
                }
                else
                {
                    cage_material_outer = Material.matchMaterial(args[1]);
                    if (cage_material_outer == null)
                    {
                        cage_material_outer = Material.GLASS;
                    }
                }
            }

            if (args.length >= 3)
            {
                if (args[2].equalsIgnoreCase("water"))
                {
                    cage_material_inner = Material.WATER;
                }
                else if (args[2].equalsIgnoreCase("lava"))
                {
                    cage_material_inner = Material.LAVA;
                }
            }

            Location target_pos = p.getLocation().add(0, 1, 0);
            playerdata.setCaged(true, target_pos, cage_material_outer, cage_material_inner);
            playerdata.regenerateHistory();
            playerdata.clearHistory();
            TFM_Util.buildHistory(target_pos, 2, playerdata);
            TFM_Util.generateCube(target_pos, 2, playerdata.getCageMaterial(TFM_UserInfo.CageLayer.OUTER));
            TFM_Util.generateCube(target_pos, 1, playerdata.getCageMaterial(TFM_UserInfo.CageLayer.INNER));

            p.setGameMode(GameMode.SURVIVAL);

            TFM_Util.bcastMsg(sender.getName() + " caged " + p.getName() + "!", ChatColor.YELLOW);
        }
        else
        {
            sender.sendMessage(TotalFreedomMod.MSG_NO_PERMS);
        }

        return true;
    }
}
