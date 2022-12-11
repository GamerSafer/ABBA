package com.gamersafer.minecraft.abbacaving.datasource;

import com.gamersafer.minecraft.abbacaving.game.GamePlayer;
import java.util.Map;
import org.bukkit.Material;

public interface PlayerDataSource {

    void init();

    void loadPlayerStats(final GamePlayer gp);

    void savePlayerStats(final GamePlayer gp);

    Map<Integer, Material> loadInventoryLayout(final GamePlayer gp);

    void saveInventoryLayout(final GamePlayer gp);

}
