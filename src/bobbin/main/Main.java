package bobbin.main;

import bobbin.boundaries.Door;
import bobbin.boundaries.Room;
import bobbin.characters.NonPlayerCharacter;
import bobbin.characters.PlayerCharacter;
import bobbin.constants.Items;
import bobbin.effects.GameCharacterEffect;
import bobbin.interaction.ConsolePrompt;
import bobbin.interaction.ExitToException;
import bobbin.interaction.Interactive;
import bobbin.interaction.actions.Action;
import bobbin.interaction.actions.BaseAction;
import bobbin.items.BaseGameEntity;
import bobbin.menus.MainMenu;
import bobbin.situations.SituationNode;
import bobbin.situations.SituationRoot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Main {

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter writer = new PrintWriter(System.out);

        start(reader, writer, MainMenu.dummyPlayerCharacter());
    }

    private static void start(
            BufferedReader reader, PrintWriter writer,
            PlayerCharacter playerCharacter) {
        PlayerCharacter actor = playerCharacter;
        MainMenu mainMenu = new MainMenu();
        Interactive next = mainMenu;

        while (true) {
            try {
                next.interact(actor, null, reader, writer);
            }
            catch (Interactive.ResetStackException e) {
                next = e.then;
                actor = e.actor;
            }
            catch (MainMenu.ExitToMainMenuException e) {
                mainMenu.saveGame(writer, playerCharacter);
                next = mainMenu;
            }
            catch (ExitToException e) {
                System.out.print("Exiting.");
                break;
            }
        }
    }

    /**
     * Build a room, allowing the player to configure their character.
     *
     * @param reader player input
     * @param writer player output
     *
     * @return {@link PlayerCharacter} in the new game.
     */
    public static PlayerCharacter buildNewGame(BufferedReader reader, PrintWriter writer) {
        Room startingRoom = new Room("Starting Room", "A Whole New Room");
        return new PlayerCharacter(
                ConsolePrompt.getResponseString(reader, writer, "Character Name"),
                ConsolePrompt.getResponseString(reader, writer, "Character Tagline"),
                startingRoom);
    }

    @SuppressWarnings("ConstantConditions")
    public static BaseGameEntity buildStockGame() {
        Room startingRoom = new Room("Starting Room", "The room you start in",
                                     Items.getCopiesOf(Items.BLUEBERRY, Items.BED));
        Room otherRoom = new Room("Another Room", "Not the room you start in",
                                  Items.getCopiesOf(Items.FLOUR));

        Door door = new Door("A door", "Door between starting room and other room", true,
                             startingRoom, otherRoom);

        NonPlayerCharacter nonPlayerCharacter = new NonPlayerCharacter(
                "Non Player Character",
                "An NPC, initially in Room 2.",
                otherRoom,
                new SituationRoot().addChildNodes(
                        new SituationNode("Greetings, stranger.", "(no response)",
                                          GameCharacterEffect.NULL,
                                          new BaseAction("Okay...", "[growling]", Action.NULL)),
                        new SituationNode("You are mean.", "Yes. Yes I am.",
                                          GameCharacterEffect.CLEAR_INVENTORY, Action.NULL)),
                Items.getCopyOf(Items.WATER).get(),
                door.makeKey("Mysterious Key", "Must fit something."));

        final PlayerCharacter pc = new PlayerCharacter(
                "The Mighty Whitey", "It's you.", startingRoom,
                Items.getCopyOf(Items.WATER).get(),
                Items.getCopyOf(Items.FLOUR).get(),
                door.makeKey("Key to a door", "Might open something"));

        return new BaseGameEntity() {
            @Override
            public int respondToInteraction(
                    PlayerCharacter actor, BaseGameEntity from,
                    BufferedReader reader, PrintWriter writer)
                    throws ExitToException {
                throw new ResetStackException(pc, pc);
            }
        };
    }

}