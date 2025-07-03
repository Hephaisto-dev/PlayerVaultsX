package com.drtshock.playervaults.vaultmanagement.storage;

import org.bukkit.inventory.Inventory;

import java.util.Set;

public interface VaultStorage {
    void saveVault(String holder, int number, Inventory inventory);

    Inventory loadVault(String holder, int number, int size);

    boolean vaultExists(String holder, int number);

    Set<Integer> getVaultNumbers(String holder);

    void deleteVault(String holder, int number);

    void deleteAllVaults(String holder);

    /**
     * Caches the vault for a player.
     * Should be run asynchronously.
     *
     * @param holder The player's unique identifier.
     */
    void cachePlayerVault(String holder);

    void removePlayerCachedVault(String holder);
}
