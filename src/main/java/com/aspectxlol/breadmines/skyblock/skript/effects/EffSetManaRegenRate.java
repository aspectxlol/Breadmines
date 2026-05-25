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
 * Effect: Set mana regen rate for a player
 * Syntax: set [the] mana regen [rate] of %player% to %number%
 */
@Name("Set Player Mana Regen Rate")
@Description("Set the mana regeneration rate (per tick) for a player. Use 0 to disable, negative to drain.")
@Examples({
    "set mana regen of player to 2.5",
    "set the mana regen rate of target player to 0",
    "set mana regen of all players to 1.5"
})
@Since("1.0")
public class EffSetManaRegenRate extends Effect {

    private Expression<Player> playerExpr;
    private Expression<Number> regenExpr;

    static {
        Skript.registerEffect(EffSetManaRegenRate.class,
                "set [the] mana regen[eration] [rate] of %players% to %number%",
                "set %players%'[s] mana regen[eration] [rate] to %number%");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.playerExpr = (Expression<Player>) exprs[0];
        this.regenExpr = (Expression<Number>) exprs[1];
        return true;
    }

    @SuppressWarnings("null")
    @Override
    protected void execute(Event event) {
        Breadmines plugin = Breadmines.getPlugin(Breadmines.class);

        Number regenAmount = regenExpr.getSingle(event);
        if (regenAmount == null) return;

        for (Player player : playerExpr.getAll(event)) {
            if (player == null) continue;
            plugin.getManaManager().setManaRegenRate(player, regenAmount.doubleValue());
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "set mana regen of " + playerExpr.toString(event, debug) + " to " + regenExpr.toString(event, debug);
    }
}
