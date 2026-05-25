package com.aspectxlol.breadmines.skyblock.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import com.aspectxlol.breadmines.Breadmines;
import org.bukkit.entity.Player;

/**
 * Expression: Get mana regen rate of a player
 * Syntax: [the] mana regen [rate] of %player%
 * Return type: number
 */
@Name("Player Mana Regen Rate")
@Description("Get the mana regeneration rate (per tick) of a player.")
@Examples("send \"Mana Regen: %mana regen of player% per tick\" to player")
@Since("1.0")
public class ExprPlayerManaRegenRate extends SimplePropertyExpression<Player, Number> {

    static {
        register(ExprPlayerManaRegenRate.class, Number.class, "mana regen[eration] [rate]", "players");
    }

    @Override
    public Number convert(Player player) {
        Breadmines plugin = Breadmines.getPlugin(Breadmines.class);
        if (plugin == null) return 0.0;
        return plugin.getManaManager().getManaRegenRate(player);
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "mana regen rate";
    }
}
