package me.StevenLawson.TotalFreedomMod.Listener;

import me.StevenLawson.TotalFreedomMod.TotalFreedomMod;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.*;

public class TFM_WeatherListener implements Listener
{
    private TotalFreedomMod plugin;

    public TFM_WeatherListener(TotalFreedomMod instance)
    {
        this.plugin = instance;
    }
    
    @EventHandler
    public void onThunderChange(ThunderChangeEvent event)
    {
        if (event.toThunderState() && TotalFreedomMod.disableWeather)
        {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event)
    {
        if (event.toWeatherState() && TotalFreedomMod.disableWeather)
        {
            event.setCancelled(true);
            return;
        }
    }
}
