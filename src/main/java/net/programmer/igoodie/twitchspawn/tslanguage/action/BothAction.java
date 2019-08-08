package net.programmer.igoodie.twitchspawn.tslanguage.action;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.programmer.igoodie.twitchspawn.configuration.ConfigManager;
import net.programmer.igoodie.twitchspawn.tslanguage.EventArguments;
import net.programmer.igoodie.twitchspawn.tslanguage.keyword.TSLActionKeyword;
import net.programmer.igoodie.twitchspawn.tslanguage.parser.TSLParser;
import net.programmer.igoodie.twitchspawn.tslanguage.parser.TSLSyntaxError;

import java.util.List;

public class BothAction extends ChainableAction {

    private boolean instant;

    public BothAction(List<String> words) throws TSLSyntaxError {
        super("AND");

        this.instant = words.get(0).equalsIgnoreCase("INSTANTLY");

        List<String> actionWords = actionPart(words);

        if (instant) {
            this.message = TSLParser.parseMessage(words);
            parseActions(actionWords.subList(1, actionWords.size()));
            this.actions.forEach(action -> action.silent = true);

        } else {
            this.silent = true; // No notification for BOTH INSTANTLY action
            parseActions(words);
        }

        if (this.actions.size() < 2)
            throw new TSLSyntaxError("Expected at least 2 actions, found -> " + this.actions.size());
    }

    @Override
    protected void performAction(ServerPlayerEntity player, EventArguments args) {
        if (instant) { // Perform them all instantly
            this.actions.forEach(action -> action.performAction(player, args));

        } else {
            this.actions.get(0).process(args); // Perform first one immediately
            this.actions.subList(1, this.actions.size()) // Queue rest of it
                    .forEach(action -> ConfigManager.RULESET_COLLECTION.queue(() -> action.process(args)));
        }
    }

}
