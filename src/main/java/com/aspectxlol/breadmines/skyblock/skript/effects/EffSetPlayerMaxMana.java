package com.aspectxlol.breadmines.skyblock.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.aspectxlol.breadmines.Breadmines;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Effect: Set player max mana
 * Syntax: set [the] max mana of %player% to %number%
 */
@Name("Set Player Max Mana")
@Description("Set a player's maximum mana amount. If current mana exceeds the new max, it gets clamped.")
@Examples({"set max mana of player to 1500", "set the max mana of target player to 2000"})
@Since("1.0")
public class EffSetPlayerMaxMana extends Effect {

    private Expression<Player> playerExpr;
    private Expression<Number> maxManaExpr;

    static {
        Skript.registerEffect(EffSetPlayerMaxMana.class,
                "set [the] max mana of %players% to %number%",
                "set %players%'[s] max mana to %number%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.playerExpr = (Expression<Player>) exprs[0];
        this.maxManaExpr = (Expression<Number>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Breadmines plugin = Breadmines.getPlugin(Breadmines.class);
        if (plugin == null) return;

        for (Player player : playerExpr.getAll(event)) {
            if (player == null) continue;
            Number maxManaAmount = maxManaExpr.getSingle(event);
            if (maxManaAmount == null) continue;
            
            plugin.getManaManager().setMaxMana(player, maxManaAmount.doubleValue());
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "set max mana of " + playerExpr.toString(event, debug) + " to " + maxManaExpr.toString(event, debug);
    }
}
