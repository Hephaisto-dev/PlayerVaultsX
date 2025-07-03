package com.drtshock.playervaults.vaultmanagement.storage;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.vaultmanagement.CardboardBoxSerialization;
import com.drtshock.playervaults.vaultmanagement.VaultHolder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class YamlVaultStorage implements VaultStorage {

    private static final String VAULTKEY = "vault%d";
    private final File directory = PlayerVaults.getInstance().getVaultData();
    private final Map<String, YamlConfiguration> cachedVaultFiles = new ConcurrentHashMap<>();

    @Override
    public void saveVault(String holder, int number, Inventory inventory) {
        YamlConfiguration yaml = getPlayerVaultFile(holder);
        String serialized = CardboardBoxSerialization.toStorage(inventory, holder);
        yaml.set(String.format(VAULTKEY, number), serialized);
        saveFileSync(holder, yaml);
    }

    @Override
    public Inventory loadVault(String holder, int number, int size) {
        if (size <= 0 || size % 9 != 0) {
            size = PlayerVaults.getInstance().getDefaultVaultSize();
        }
        YamlConfiguration playerFile = getPlayerVaultFile(holder);
        String serialized = playerFile.getString(String.format(VAULTKEY, number));
        if (serialized == null) {
            return null;
        }
        ItemStack[] contents = CardboardBoxSerialization.fromStorage(serialized, holder);
        Inventory inventory = Bukkit.createInventory(new VaultHolder(number), size, PlayerVaults.getInstance().getVaultTitle(String.valueOf(number)));
        if (contents != null) {
            if (contents.length > size) {
                for (ItemStack stack : contents) {
                    if (stack != null) {
                        inventory.addItem(stack);
                    }
                }
            } else {
                inventory.setContents(contents);
            }
        }
        return inventory;
    }

    @Override
    public boolean vaultExists(String holder, int number) {
        File file = new File(directory, holder + ".yml");
        if (!file.exists()) {
            return false;
        }
        return getPlayerVaultFile(holder).contains(String.format(VAULTKEY, number));
    }

    @Override
    public Set<Integer> getVaultNumbers(String holder) {
        Set<Integer> vaults = new HashSet<>();
        YamlConfiguration file = getPlayerVaultFile(holder);
        if (file == null) {
            return vaults;
        }
        for (String s : file.getKeys(false)) {
            try {
                if (s.startsWith("vault")) {
                    int number = Integer.parseInt(s.substring(5));
                    vaults.add(number);
                }
            } catch (NumberFormatException e) {
                // Ignore invalid keys
            }
        }
        return vaults;
    }

    @Override
    public void deleteVault(String holder, int number) {
        File file = new File(directory, holder + ".yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration playerFile = YamlConfiguration.loadConfiguration(file);
        playerFile.set(String.format(VAULTKEY, number), null);
        if (cachedVaultFiles.containsKey(holder)) {
            cachedVaultFiles.put(holder, playerFile);
        }
        try {
            playerFile.save(file);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void deleteAllVaults(String holder) {
        removePlayerCachedVault(holder);
        deletePlayerVaultFile(holder);
    }

    @Override
    public void cachePlayerVault(String holder) {
        YamlConfiguration config = this.loadPlayerVaultFile(holder, false);
        if (config != null) {
            this.cachedVaultFiles.put(holder, config);
        }
    }

    @Override
    public void removePlayerCachedVault(String holder) {
        cachedVaultFiles.remove(holder);
    }

    private YamlConfiguration getPlayerVaultFile(String holder) {
        if (cachedVaultFiles.containsKey(holder)) {
            return cachedVaultFiles.get(holder);
        }
        return loadPlayerVaultFile(holder, true);
    }

    private YamlConfiguration loadPlayerVaultFile(String holder, boolean createIfNotFound) {
        if (!this.directory.exists()) {
            boolean createdDirs = this.directory.mkdir();
            if (createdDirs) {
                PlayerVaults.debug("Created vault directory: " + this.directory.getAbsolutePath());
            } else {
                PlayerVaults.getInstance().getLogger().warning("Failed to create vault directory: " + this.directory.getAbsolutePath());
            }
        }
        File file = new File(this.directory, holder + ".yml");
        if (!file.exists()) {
            if (createIfNotFound) {
                try {
                    boolean created = file.createNewFile();
                    if (created) {
                        PlayerVaults.debug("Created vault file for " + holder);
                    } else {
                        PlayerVaults.getInstance().getLogger().warning("Vault file already exists for " + holder);
                    }
                } catch (IOException e) {
                    PlayerVaults.getInstance().addException(new IllegalStateException("Failed to create vault file for: " + holder, e));
                }
            } else {
                return null;
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void deletePlayerVaultFile(String holder) {
        File file = new File(this.directory, holder + ".yml");
        if (file.exists()) {
            boolean delete = file.delete();
            if (!delete) {
                PlayerVaults.getInstance().addException(new IllegalStateException("Failed to delete vault file for: " + holder));
                PlayerVaults.getInstance().getLogger().severe("Failed to delete vault file for: " + holder);
            } else {
                PlayerVaults.debug("Deleted vault file for " + holder);
            }
        }
    }

    private void saveFileSync(final String holder, final YamlConfiguration yaml) {
        if (cachedVaultFiles.containsKey(holder)) {
            cachedVaultFiles.put(holder, yaml);
        }

        final boolean backups = PlayerVaults.getInstance().isBackupsEnabled();
        final File backupsFolder = PlayerVaults.getInstance().getBackupsFolder();
        final File file = new File(directory, holder + ".yml");
        if (file.exists() && backups) {
            boolean renamedTo = file.renameTo(new File(backupsFolder, holder + ".yml"));
            if (!renamedTo) {
                PlayerVaults.getInstance().addException(new IllegalStateException("Failed to rename vault file for: " + holder));
                PlayerVaults.getInstance().getLogger().log(Level.SEVERE, "Failed to rename vault file for: " + holder);
            } else {
                PlayerVaults.debug("Renamed vault file for " + holder + " to backups folder.");
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            PlayerVaults.getInstance().addException(new IllegalStateException("Failed to save vault file for: " + holder, e));
            PlayerVaults.getInstance().getLogger().log(Level.SEVERE, "Failed to save vault file for: " + holder, e);
        }
        PlayerVaults.debug("Saved vault for " + holder);
    }
}
