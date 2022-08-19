package org.OxYura;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;

import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;

import org.bukkit.entity.Player;
import org.bukkit.Location;

import org.bukkit.Material;
import org.bukkit.World;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.ProtocolLibrary;

import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.ListenerPriority;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;

import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;


public final class AsyncXrayBypassFix extends JavaPlugin implements Listener {

    boolean f(Material m) {

        if (m != Material.STONE)
            if (m != Material.IRON_ORE)
                if (m != Material.GOLD_ORE)
                    if (m != Material.DIAMOND_ORE)
                        return false;
        return true;
    }

    World w;

    boolean unlegal(int x, int y, int z) {

        if (f(w.getBlockAt(x + 1, y, z).getType()))
            if (f(w.getBlockAt(x - 1, y, z).getType()))
                if (f(w.getBlockAt(x, y + 1, z).getType()))
                    if (f(w.getBlockAt(x, y - 1, z).getType()))
                        if (f(w.getBlockAt(x, y, z + 1).getType()))
                            if (f(w.getBlockAt(x, y, z - 1).getType()))
                                return true;
        return false;
    }

    boolean unlegal_receive(PacketEvent e) {
        Player p = e.getPlayer();
        Location loc = p.getLocation();

        double px = loc.getX();
        double py = loc.getY();
        double pz = loc.getZ();

        BlockPosition b = e.getPacket().getBlockPositionModifier().read(0);

        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();

        if ((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz) > 64.0)
            return true;

        if (p.getWorld().getName().equals("world"))
            return unlegal(x, y, z);

        return false;
    }

    @Override
    public void onEnable() {
        w = Bukkit.getWorld("world");
        org.spigotmc.AsyncCatcher.enabled = false;
        ProtocolManager PM = ProtocolLibrary.getProtocolManager();

        FileConfiguration conf = getConfig();
        if (!new File(getDataFolder() + File.separator + "config.yml").exists()) {
            conf.options().copyDefaults(true);
            saveDefaultConfig();
        }

        if (conf.isBoolean("check-receiving"))
            if (conf.getBoolean("check-receiving")) {
                PM.addPacketListener(new PacketAdapter(
                        this,
                        ListenerPriority.HIGHEST,
                        PacketType.Play.Client.BLOCK_DIG
                ) {
                    @Override
                    public void onPacketReceiving(PacketEvent e) {
                        if (e.getPacket().getPlayerDigTypes().read(0).ordinal() <= 0x02)
                            if (unlegal_receive(e))
                                e.setCancelled(true);
                    }
                });

                PM.addPacketListener(new PacketAdapter(
                        this,
                        ListenerPriority.HIGHEST,
                        PacketType.Play.Client.USE_ITEM
                ) {
                    @Override
                    public void onPacketReceiving(PacketEvent e) {
                        if (unlegal_receive(e))
                            e.setCancelled(true);
                    }
                });
            }

        if (conf.isBoolean("check-sending"))
            if (conf.getBoolean("check-sending")) {
                PM.addPacketListener(new PacketAdapter(
                        this,
                        ListenerPriority.HIGHEST,
                        PacketType.Play.Server.BLOCK_CHANGE
                ) {
                    @Override
                    public void onPacketSending(PacketEvent e) {
                        if (e.getPlayer().getWorld().getName().equals("world")) {
                            BlockPosition b = e.getPacket().getBlockPositionModifier().read(0);
                            if (unlegal(b.getX(), b.getY(), b.getZ()))
                                e.setCancelled(true);
                        }
                    }
                });

                PM.addPacketListener(new PacketAdapter(
                        this,
                        ListenerPriority.HIGHEST,
                        PacketType.Play.Server.MULTI_BLOCK_CHANGE
                ) {
                    @Override
                    public void onPacketSending(PacketEvent e) {
                        if (e.getPlayer().getWorld().getName().equals("world")) {
                            MultiBlockChangeInfo[] m = e.getPacket().getMultiBlockChangeInfoArrays().read(0);
                            for (MultiBlockChangeInfo b : m)
                                if (unlegal(b.getAbsoluteX(), b.getY(), b.getAbsoluteZ())) {
                                    e.setCancelled(true);
                                    return;
                                }
                        }
                    }
                });
            }
    }
}