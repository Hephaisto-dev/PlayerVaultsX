/*
 * PlayerVaultsX
 * Copyright (C) 2013 Trent Hensler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.drtshock.playervaults.vaultmanagement;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.vaultmanagement.storage.VaultStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;

public class VaultManager {
    private final VaultStorage storage;
    private final PlayerVaults plugin;

    public VaultManager(PlayerVaults plugin, VaultStorage storage) {
        this.storage = storage;
        this.plugin = plugin;
    }

    /**
     * Saves the inventory to the specified player and vault number.
     *
     * @param inventory The inventory to be saved.
     * @param target    The player.
     * @param number    The vault number.
     */
    public void saveVault(Inventory inventory, String target, int number) {
        storage.saveVault(target, number, inventory);
    }

    /**
     * Load the player's vault and return it.
     *
     * @param player The holder of the vault.
     * @param number The vault number.
     */
    public Inventory loadOwnVault(Player player, int number, int size) {
        if (size % 9 != 0) {
            size = PlayerVaults.getInstance().getDefaultVaultSize();
        }

        PlayerVaults.debug("Loading self vault for " + player.getName() + " (" + player.getUniqueId() + ')');

        String title = PlayerVaults.getInstance().getVaultTitle(String.valueOf(number));
        VaultViewInfo info = new VaultViewInfo(player.getUniqueId().toString(), number);
        if (PlayerVaults.getInstance().getOpenInventories().containsKey(info.toString())) {
            PlayerVaults.debug("Already open");
            return PlayerVaults.getInstance().getOpenInventories().get(info.toString());
        }

        String holder = player.getUniqueId().toString();
        Inventory inv = storage.loadVault(holder, number, size);
        if (inv == null) {
            PlayerVaults.debug("No vault matching number");
            VaultHolder vaultHolder = new VaultHolder(number);
            inv = Bukkit.createInventory(vaultHolder, size, title);
            vaultHolder.setInventory(inv);
        }

        return inv;
    }

    /**
     * Load the player's vault and return it.
     *
     * @param name   The holder of the vault.
     * @param number The vault number.
     */
    public Inventory loadOtherVault(String name, int number, int size) {
        if (size % 9 != 0) {
            size = PlayerVaults.getInstance().getDefaultVaultSize();
        }

        PlayerVaults.debug("Loading other vault for " + name);

        String holder = name;

        try {
            UUID uuid = UUID.fromString(name);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            holder = offlinePlayer.getUniqueId().toString();
        } catch (Exception e) {
            // Not a player
        }

        VaultViewInfo info = new VaultViewInfo(name, number);
        Inventory inv;
        if (PlayerVaults.getInstance().getOpenInventories().containsKey(info.toString())) {
            PlayerVaults.debug("Already open");
            inv = PlayerVaults.getInstance().getOpenInventories().get(info.toString());
        } else {
            inv = storage.loadVault(holder, number, size);
            if (inv == null) {
                return null;
            }
            PlayerVaults.getInstance().getOpenInventories().put(info.toString(), inv);
        }
        return inv;
    }

    /**
     * Gets an inventory without storing references to it. Used for dropping a players inventories on death.
     *
     * @param holder The holder of the vault.
     * @param number The vault number.
     * @return The inventory of the specified holder and vault number. Can be null.
     */
    public Inventory getVault(String holder, int number) {
        return storage.loadVault(holder, number, -1);
    }

    /**
     * Checks if a vault exists.
     *
     * @param holder holder of the vault.
     * @param number vault number.
     * @return true if the vault and vault number exist, otherwise false.
     */
    public boolean vaultExists(String holder, int number) {
        return storage.vaultExists(holder, number);
    }

    /**
     * Gets the numbers belonging to all their vaults.
     *
     * @param holder holder
     * @return a set of Integers, which are player's vaults' numbers (fuck grammar).
     */
    public Set<Integer> getVaultNumbers(String holder) {
        return storage.getVaultNumbers(holder);
    }

    public void deleteAllVaults(String holder) {
        storage.deleteAllVaults(holder);
    }

    /**
     * Deletes a players vault.
     *
     * @param sender The sender of whom to send messages to.
     * @param holder The vault holder.
     * @param number The vault number.
     */
    public void deleteVault(CommandSender sender, final String holder, final int number) {
        new BukkitRunnable() {
            @Override
            public void run() {
                storage.deleteVault(holder, number);
            }
        }.runTaskAsynchronously(PlayerVaults.getInstance());

        OfflinePlayer player = Bukkit.getPlayer(holder);
        if (player != null) {
            if (sender.getName().equalsIgnoreCase(player.getName())) {
                this.plugin.getTL().deleteVault().title().with("vault", String.valueOf(number)).send(sender);
            } else {
                this.plugin.getTL().deleteOtherVault().title().with("vault", String.valueOf(number)).with("player", player.getName()).send(sender);
            }
        }

        String vaultName = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : holder;
        PlayerVaults.getInstance().getOpenInventories().remove(new VaultViewInfo(vaultName, number).toString());
    }

    public void cachePlayerVault(String holder) {
        storage.cachePlayerVault(holder);
    }

    public void removeCachedPlayerVault(String holder) {
        storage.removePlayerCachedVault(holder);
    }
}
