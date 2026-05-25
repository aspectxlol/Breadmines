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
 * Effect: Add or remove mana from a player
 * Syntax: (add|give) %number% mana to %player%
 *         (remove|subtract) %number% mana from %player%
 */
@Name("Add or Remove Player Mana")
@Description("Add or remove mana from a player (respects min/max bounds).")
@Examples({
    "add 50 mana to player",
    "give 100 mana to target player",
    "remove 25 mana from player",
    "subtract 75 mana from all players"
})
@Since("1.0")
public class EffAddRemovePlayerMana extends Effect {

    private Expression<Number> manaExpr;
    private Expression<Player> playerExpr;
    private boolean isAdd;

    static {
        Skript.registerEffect(EffAddRemovePlayerMana.class,
                "(add|give) %number% mana to %players%",
                "(remove|subtract) %number% mana from %players%");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.manaExpr = (Expression<Number>) exprs[0];
        this.playerExpr = (Expression<Player>) exprs[1];
        this.isAdd = matchedPattern == 0; // 0 = add/give, 1 = remove/subtract
        return true;
    }

    @Override
    protected void execute(Event event) {
        Breadmines plugin = Breadmines.getPlugin(Breadmines.class);

        Number manaAmount = manaExpr.getSingle(event);
        if (manaAmount == null) return;

        double amount = manaAmount.doubleValue();
        if (!isAdd) {
            amount = -amount; // Convert subtract to negative add
        }

        for (Player player : playerExpr.getAll(event)) {
            if (player == null) continue;
            plugin.getManaManager().addMana(player, amount);
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        String action = isAdd ? "add" : "remove";
        return action + " " + manaExpr.toString(event, debug) + " mana to/from " + playerExpr.toString(event, debug);
    }
}
