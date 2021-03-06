package bobbin.interaction;

import bobbin.boundaries.Room;
import bobbin.characters.GameCharacter;
import bobbin.characters.PlayerCharacter;
import bobbin.constants.Actions;
import bobbin.interaction.actions.ActionList;
import bobbin.interaction.console.Console;
import bobbin.items.BaseGameEntity;
import bobbin.items.GameEntity;

import java.util.Objects;

public abstract class Interactive {

    public static class ExitToSystemException extends ExitToException {}

    /**
     * Get the set of actions to present to the player when they {@link #interact(PlayerCharacter, BaseGameEntity, Console)} with this object. Depending on needs, the base
     * list should be fetched by subclasses of {@link Interactive}, which should then add to that
     * list.
     *
     * @param actor {@link GameCharacter} interacting with {@link this}
     * @param from  {@link GameEntity} that the {@link GameCharacter} came from
     * @return the base set of player actions
     */
    protected ActionList actions(GameCharacter actor, BaseGameEntity from) {
        ActionList actions = new ActionList();
        actions.add(Actions.BACK);
        return actions;
    }

    /**
     * Respond to interaction from another {@link GameEntity}.
     *
     * @param from   source of the interaction.
     * @param console to output text and read response from
     * @return the number of levels to go up from the current stage.
     */
    public abstract int respondToInteraction(
            PlayerCharacter actor, BaseGameEntity from,
            Console console) throws ExitToException;

    /**
     * The visibility of an {@link Interactive} item to the player.
     */
    public enum Visibility {
        /**
         * The player has never seen the {@link Interactive}.
         */
        FRESH(1000),

        /**
         * The player has seen the {@link Interactive}, but not visited it.
         */
        SEEN(100),

        /**
         * The player has visited the {@link Interactive}, but the {@link Interactive} can be
         * revisited by the player.
         */
        VISITED(10),

        /**
         * The {@link Interactive} is hidden to the player, either because it only allows a single
         * visit, or because the game logic has hidden it for another reason.
         */
        HIDDEN(0);

        /**
         * The "visibility level" of the {@link Interactive}. Higher numbers imply more visibility
         * or relevance to the player, lower levels imply less visibility or relevance to the player.
         * Numerical values serve only to establish a hierarchy, and have no other significance.
         */
        final int level;

        Visibility(int level) {
            this.level = level;
        }

        public Visibility min(Visibility visibility1, Visibility visibility2) {
            return visibility1.level < visibility2.level ? visibility1 : visibility2;
        }

        /**
         * Return the lowest {@link Visibility} between {@link this} and the given {@link Visibility}.
         *
         * @param otherVisibility the other {@link Visibility} to compare
         * @return the lowest {@link Visibility}
         */
        public Visibility min(Visibility otherVisibility) {
            return min(this, otherVisibility);
        }
    }

    /**
     * Should be caught at the highest level possible (e.g. the main game loop) which should, after
     * catching it, call {@link #then#interact(PlayerCharacter, BaseGameEntity, Console)}.
     * <p>
     * This will allow for the stack to be reset whenever moving from one {@link Interactive} to
     * another is not necessarily a parent-child jump. For example, when moving between {@link Room}s,
     * one {@link Room} wouldn't be considered the parent or the child of the other.
     * <p>
     * This is to allow for continuous use of Java's stack as a method of "tree" traversal and state
     * management without the risk of exceeding the recursion limit.
     */
    public static class ResetStackException extends ExitToException {

        public final PlayerCharacter actor;
        public final Interactive then;

        public ResetStackException(PlayerCharacter actor, Interactive then) {
            this.actor = actor;
            this.then = then;
        }
    }

    /**
     * Class of shortcuts for return statements in {@link #interact(PlayerCharacter, BaseGameEntity, Console)}, {@link
     * #respondToInteraction(PlayerCharacter, BaseGameEntity, Console)}, and the like. Makes the semantics of the return
     * statements in these methods more reader-friendly.
     */
    protected static class GoTo {

        /**
         * When execution ends, return to the current {@link Interactive} (looping interaction).
         */
        public static final int THIS = 0;

        /**
         * When execution ends, return to the parent {@link Interactive}.
         */
        public static final int PARENT = 1;

        /**
         * When interaction ends, return to the grandparent {@link Interactive}.
         */
        public static final int GRANDPARENT = 2;
    }

    /**
     * Every {@link Interactive} starts off with {@link Visibility#FRESH}, as it has not been seen by
     * the player yet.
     */
    private Visibility visibility = Visibility.FRESH;

    /**
     * @return visibility of {@link this}.
     * @see Visibility
     */
    public Visibility getVisibility() {
        return visibility;
    }

    private void mark(Visibility visibility) {
        this.visibility = this.visibility.min(visibility);
    }

    /**
     * Mark {@link this} as being seen by the {@link PlayerCharacter}.
     */
    public void markSeen() {
        mark(Visibility.SEEN);
    }

    /**
     * Mark {@link this} as being hidden from the {@link PlayerCharacter}.
     */
    public void markHidden() {
        mark(Visibility.HIDDEN);
    }

    /**
     * Mark {@link this} as being visited by the {@link PlayerCharacter}.
     */
    public void markVisited() {
        mark(Visibility.VISITED);
    }

    public int interact(
            PlayerCharacter actor, BaseGameEntity from,
            Console console) throws ExitToException {
        Objects.requireNonNull(actor);
        Objects.requireNonNull(console);

        markVisited();

        int desiredHeight;
        do {
            desiredHeight = respondToInteraction(actor, from, console);
        }
        while (desiredHeight == GoTo.THIS);

        if (desiredHeight < 0) {
            throw new IllegalStateException(String.format("negative height (%d) found", desiredHeight));
        }

        return desiredHeight - 1;
    }

    public void resetStackAndInteract(PlayerCharacter actor) throws ResetStackException {
        throw new ResetStackException(actor, this);
    }
}
