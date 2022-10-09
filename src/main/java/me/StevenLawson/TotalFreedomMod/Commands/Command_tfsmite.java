package me.StevenLawson.TotalFreedomMod.Commands;

import java.util.ArrayList;
import java.util.List;
import me.StevenLawson.TotalFreedomMod.TFM_UserInfo;
import me.StevenLawson.TotalFreedomMod.TFM_Util;
import me.StevenLawson.TotalFreedomMod.TotalFreedomMod;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Command_tfsmite extends TFM_Command
{
    @Override
    public boolean run(CommandSender sender, Player sender_p, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 1)
        {
            return false;
        }

        if (senderIsConsole || TFM_Util.isUserSuperadmin(sender, plugin))
        {
            List<Player> targets = new ArrayList<>();

            if (args[0].equalsIgnoreCase("all"))
            {
                targets.addAll(Bukkit.getOnlinePlayers());
            }
            else
            {
                List<Player> matches = Bukkit.matchPlayer(args[0]);
                if (matches.isEmpty())
                {
                    sender.sendMessage("Can't find user " + args[0]);
                    return true;
                }
                else
                {
                    targets.add(matches.get(0));
                }
            }

            for (Player p : targets)
            {
                TFM_Util.bcastMsg(p.getName() + " has been a naughty, naughty boy.", ChatColor.RED);

                //Deop
                p.setOp(false);

                //Set gamemode to survival:
                p.setGameMode(GameMode.SURVIVAL);

                //Clear inventory:
                p.getInventory().clear();

                //Flag for insta-kill:
                TFM_UserInfo playerdata = TFM_UserInfo.getPlayerData(p);
                playerdata.setForcedDeath(true);

                //Strike with lightning effect:
                final Location target_pos = p.getLocation();
                for (int x = -1; x <= 1; x++)
                {
                    for (int z = -1; z <= 1; z++)
                    {
                        final Location strike_pos = new Location(target_pos.getWorld(), target_pos.getBlockX() + x, target_pos.getBlockY(), target_pos.getBlockZ() + z);
                        target_pos.getWorld().strikeLightning(strike_pos);
                    }
                }
            }
        }
        else
        {
            sender.sendMessage(TotalFreedomMod.MSG_NO_PERMS);
        }

        return true;

    }
}
