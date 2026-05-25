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
 * Expression: Get current mana of a player
 * Syntax: [the] [current] mana of %player%
 * Return type: number
 */
@Name("Player Mana")
@Description("Get the current mana amount of a player.")
@Examples("send \"Mana: %mana of player%\" to player")
@Since("1.0")
public class ExprPlayerMana extends SimplePropertyExpression<Player, Number> {

    static {
        register(ExprPlayerMana.class, Number.class, "[current] mana", "players");
    }

    @Override
    public Number convert(Player player) {
        Breadmines plugin = Breadmines.getPlugin(Breadmines.class);
        if (plugin == null) return 0.0;
        return plugin.getManaManager().getMana(player);
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "mana";
    }
}
