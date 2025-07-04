package com.drtshock.playervaults.vaultmanagement.storage;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.vaultmanagement.CardboardBoxSerialization;
import com.drtshock.playervaults.vaultmanagement.VaultHolder;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class MySQLVaultStorage implements VaultStorage {
    private final String url;
    private final String username;
    private final String password;

    public MySQLVaultStorage(String host, int port, String database, String username, String password) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
        this.username = username;
        this.password = password;
        createTableIfNotExists();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS playervaults (" +
                "holder VARCHAR(64) NOT NULL," +
                "number INT NOT NULL," +
                "size INT NOT NULL," +
                "data TEXT," +
                "PRIMARY KEY (holder, number)" +
                ")";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            PlayerVaults.getInstance().getLogger().severe("Failed to create playervaults table: " + e.getMessage());
        }
    }

    @Override
    public void saveVault(String holder, int number, Inventory inventory) {
        String sql = "REPLACE INTO playervaults (holder, number, size, data) VALUES (?, ?, ?, ?)";
        String serialized = CardboardBoxSerialization.toStorage(inventory, holder);
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, holder);
            ps.setInt(2, number);
            ps.setInt(3, inventory.getSize());
            ps.setString(4, serialized);
            ps.executeUpdate();
        } catch (SQLException e) {
            PlayerVaults.getInstance().getLogger().severe("Failed to save vault: " + e.getMessage());
        }
    }

    @Override
    public Inventory loadVault(String holder, int number, int size) {
        Inventory inventory;
        String sql = "SELECT data, size FROM playervaults WHERE holder = ? AND number = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, holder);
            ps.setInt(2, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String serialized = rs.getString("data");
                    int vaultSize = rs.getInt("size");
                    if (size <= 0 || size % 9 != 0) {
                        size = vaultSize > 0 ? vaultSize : PlayerVaults.getInstance().getDefaultVaultSize();
                    }
                    ItemStack[] contents = CardboardBoxSerialization.fromStorage(serialized, holder);
                    inventory = Bukkit.createInventory(new VaultHolder(number), size, PlayerVaults.getInstance().getVaultTitle(String.valueOf(number)));
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
            }
        } catch (SQLException e) {
            PlayerVaults.getInstance().getLogger().severe("Failed to load vault: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean vaultExists(String holder, int number) {
        String sql = "SELECT 1 FROM playervaults WHERE holder = ? AND number = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, holder);
            ps.setInt(2, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            PlayerVaults.getInstance().getLogger().severe("Failed to check vault existence: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Set<Integer> getVaultNumbers(String holder) {
        Set<Integer> vaults = new HashSet<>();
        String sql = "SELECT number FROM playervaults WHERE holder = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, holder);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vaults.add(rs.getInt("number"));
                }
            }
        } catch (SQLException e) {
            PlayerVaults.getInstance().getLogger().severe("Failed to get vault numbers: " + e.getMessage());
        }
        return vaults;
    }

    @Override
    public void deleteVault(String holder, int number) {
        String sql = "DELETE FROM playervaults WHERE holder = ? AND number = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, holder);
            ps.setInt(2, number);
            ps.executeUpdate();
        } catch (SQLException e) {
            PlayerVaults.getInstance().getLogger().severe("Failed to delete vault: " + e.getMessage());
        }
    }

    @Override
    public void deleteAllVaults(String holder) {
        String sql = "DELETE FROM playervaults WHERE holder = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, holder);
            ps.executeUpdate();
        } catch (SQLException e) {
            PlayerVaults.getInstance().getLogger().severe("Failed to delete all vaults: " + e.getMessage());
        }
    }

    @Override
    public void cachePlayerVault(String holder) {
    }

    @Override
    public void removePlayerCachedVault(String holder) {
    }
}