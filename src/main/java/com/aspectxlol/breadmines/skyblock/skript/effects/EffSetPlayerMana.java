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
 * Effect: Set player mana
 * Syntax: set [the] mana of %player% to %number%
 */
@Name("Set Player Mana")
@Description("Set a player's mana to a specific amount (clamped 0-500).")
@Examples({"set mana of player to 250", "set the mana of target player to 500"})
@Since("1.0")
public class EffSetPlayerMana extends Effect {

    private Expression<Player> playerExpr;
    private Expression<Number> manaExpr;

    static {
        Skript.registerEffect(EffSetPlayerMana.class,
                "set [the] mana of %players% to %number%",
                "set %players%'[s] mana to %number%");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.playerExpr = (Expression<Player>) exprs[0];
        this.manaExpr = (Expression<Number>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Breadmines plugin = Breadmines.getPlugin(Breadmines.class);
        if (plugin == null) return;

        for (Player player : playerExpr.getAll(event)) {
            if (player == null) continue;
            Number manaAmount = manaExpr.getSingle(event);
            if (manaAmount == null) continue;
            
            plugin.getManaManager().setMana(player, manaAmount.doubleValue());
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "set mana of " + playerExpr.toString(event, debug) + " to " + manaExpr.toString(event, debug);
    }
}
