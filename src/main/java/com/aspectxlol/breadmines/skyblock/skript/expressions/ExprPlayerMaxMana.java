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
 * Expression: Get max mana of a player
 * Syntax: [the] max mana of %player%
 * Return type: number
 */
@Name("Player Max Mana")
@Description("Get the maximum mana amount of a player.")
@Examples("send \"Max Mana: %max mana of player%\" to player")
@Since("1.0")
public class ExprPlayerMaxMana extends SimplePropertyExpression<Player, Number> {

    static {
        register(ExprPlayerMaxMana.class, Number.class, "max mana", "players");
    }

    @SuppressWarnings("null")
    @Override
    public Number convert(Player player) {
        Breadmines plugin = Breadmines.getPlugin(Breadmines.class);
        if (plugin == null) return 500.0;
        return plugin.getManaManager().getMaxMana(player);
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "max mana";
    }
}
