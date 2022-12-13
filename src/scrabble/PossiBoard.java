package scrabble;

import edu.princeton.cs.algs4.In;

import java.util.*;

/**
 * Extends the board class, can be constructed from a current board state.
 *
 * Unable to get the scoring sweep for letters at the end of the game, so assumes it will be the average, aka 0.
 */
public class PossiBoard extends Board {

    /** Width of this Board, in squares. */
    public static final int WIDTH = 15;

    /** Symbol for a double letter score square. */
    public static final char DOUBLE_LETTER_SCORE = '-';

    /** Symbol for a triple letter score square. */
    public static final char TRIPLE_LETTER_SCORE = '=';

    /** Symbol for a double word score square. */
    public static final char DOUBLE_WORD_SCORE = '+';

    /** Symbol for a triple word score square. */
    public static final char TRIPLE_WORD_SCORE = '#';

    /** Symbol for a regular square. */
    public static final char NO_PREMIUM = ' ';

    /** Set of legal words. */
    private static final HashSet<String> DICTIONARY = new HashSet<>();

    /** Associates tiles with their values. */
    public static final Map<Character, Integer> TILE_VALUES = new HashMap<>();

    static {
        // Load dictionary
        In in = new In("words.txt");
        for (String word : in.readAllLines()) {
            DICTIONARY.add(word);
        }
        // Initialize tile values
        for (char c : "eaionrtlsu".toCharArray()) {
            TILE_VALUES.put(c, 1);
        }
        for (char c : "dg".toCharArray()) {
            TILE_VALUES.put(c, 2);
        }
        for (char c : "bcmp".toCharArray()) {
            TILE_VALUES.put(c, 3);
        }
        for (char c : "fhvwy".toCharArray()) {
            TILE_VALUES.put(c, 4);
        }
        TILE_VALUES.put('k', 5);
        for (char c : "jx".toCharArray()) {
            TILE_VALUES.put(c, 8);
        }
        for (char c : "qz".toCharArray()) {
            TILE_VALUES.put(c, 10);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            TILE_VALUES.put(c, 0);
        }
        TILE_VALUES.put('_', 0);
    }

    public static ArrayList<Character> FULL_HAND = new ArrayList<Character>();

    static  {
        FULL_HAND.add('a');
        FULL_HAND.add('e');
        FULL_HAND.add('i');
        FULL_HAND.add('o');
        FULL_HAND.add('u');
        FULL_HAND.add('s');
        FULL_HAND.add('r');
        FULL_HAND.add('t');
        FULL_HAND.add('l');

    }

    /**
     * Initial layout of bonus squares.
     */
    public static String[] LAYOUT = {
            "#  -   #   -  #",
            " +   =   =   + ",
            "  +   - -   +  ",
            "-  +   -   +  -",
            "    +     +    ",
            " =   =   =   = ",
            "  -   - -   -  ",
            "#  -   +   -  #",
            "  -   - -   -  ",
            " =   =   =   = ",
            "    +     +    ",
            "-  +   -   +  -",
            "  +   - -   +  ",
            " +   =   =   + ",
            "#  -   #   -  #"};

    /** Squares on the board (whether occupied by tiles or not). */
    public char[][] squares;
    public int value;

    // initial constructor, blank board.
    public PossiBoard() {
        squares = new char[15][15];
        for (int r = 0; r < squares.length; r++) {
            for (int c = 0; c < squares.length; c++) {
                squares[r][c] = LAYOUT[r].charAt(c);
            }
        }
    }

    // Using sketchy coding practices to circumnavigate the fact that the Board class was not designed to be re-used.
    private int tmpScore = 0;

    // creates a new board from the previous one.
    public PossiBoard(PossiBoard board, ScrabbleMove move, boolean scorePositive) {

        // copy the original board's value.
        this.value = board.value;

        // deep copy the previous board's squares
        squares = new char[15][15];
        for (int r = 0; r < squares.length; r++) {
            for (int c = 0; c < squares.length; c++) {
                squares[r][c] = board.squares[r][c];
            }
        }

        // play the move on the new possiboard
        try {
            move.play(this, 0);
        } catch (Exception e) { e.printStackTrace(); }

        // years of coding in assembly-likes is finally paying off. Eat this, object-oriented coding standards.
        if (scorePositive) { this.value += tmpScore; } else { this.value -= tmpScore; }
    }

    public PossiBoard(char[][] boardLayout, ScrabbleMove move, boolean scorePositive) {

        // deep copy the previous board's squares
        this.squares = new char[15][15];
        for (int r = 0; r < Board.WIDTH; r++) {
            for (int c = 0; c < Board.WIDTH; c++) {
                this.squares[r][c] = boardLayout[r][c];
            }
        }

        // play the move on the new possiboard
        try {
            move.play(this, 0);
        } catch (Exception e) { e.printStackTrace(); }

        // possiboards using this constructor don't have an already known value, so start at 0.
        if (scorePositive) { this.value = tmpScore; } else { this.value = -tmpScore; }
    }

    /**
     * We already know this move is valid.
     */
    @Override
    public void play(String word, Location location, Location direction, List<Character> hand)
            throws IllegalMoveException {
        this.tmpScore = score(word, location, direction);
        placeWord(word, location, direction);
    }


    /**
     * If this move was supposed to exchange tiles, we simply don't care.
     */
    @Override
    public void exchange(List<Character> hand, boolean[] tilesToExchange) {
        this.tmpScore = 0;
        return;
    }


    /**
     * The ScrabbleMove play method calls this, so make sure it returns *something* that won't break.
     */
    @Override
    public List<Character> getHand(int player) {
        return FULL_HAND;
    }





    /**
     * Returns player's score.
     *
     * @param player Player number (0 or 1).
     */
    public int getScore(int player) {
        return 0;
    }

    @Override
    public String toString() {
        String result = "";
        for (int r = 0; r < squares.length; r++) {
            for (int c = 0; c < squares.length; c++) {
                result += squares[r][c];
            }
            result += "\n";
        }
        return result;
    }

    // we want words to always be playable
    public boolean canBeDrawnFromHand(String word, List<Character> hand) {
        return true;
    }

    /**
     * Returns true if word can be placed on board, in the sense of not overlapping existing tiles,
     * leaving no gaps, having no tiles right before or after it, and not extending beyond the edge of the board.
     */
    public boolean canBePlacedOnBoard(String word, Location location, Location direction) {
        // Check for tile right before word
        Location before = location.antineighbor(direction);
        if (before.isOnBoard() && isOccupied(before)) {
            return false;
        }
        // Check squares within word
        for (char c : word.toCharArray()) {
            if (!location.isOnBoard()) { // Off edge of board
                return false;
            }
            char current = getSquare(location);
            if ((c == ' ') != (Character.isAlphabetic(current))) {
                // Tile played on top of existing tile or gap in word where there is no tile
                return false;
            }
            location = location.neighbor(direction);
        }
        // Check for tile right after word
        if (location.isOnBoard() && isOccupied(location)) {
            return false;
        }
        // No problems!
        return true;
    }

    /**
     * Returns the letter or symbol at location.
     */
    public char getSquare(Location location) {
        return squares[location.getRow()][location.getColumn()];
    }

    /**
     * Sets the letter or symbol at location.
     */
    private void setSquare(char tile, Location location) {
        squares[location.getRow()][location.getColumn()] = tile;
    }

    /**
     * Places word on board at the specified location and direction. Assumes this is legal.
     */
    public void placeWord(String word, Location location, Location direction) {
        for (char c : word.toCharArray()) {
            if (c != ' ') {
                setSquare(c, location);
            }
            location = location.neighbor(direction);
        }
    }

    /**
     * Returns true if word, placed at location in direction, would be connected. In other words,
     * word must contain an existing tile, be beside an existing tile, or contain the center.
     */
    public boolean wouldBeConnected(String word, Location location, Location direction) {
        Location cross = direction.opposite();
        for (char c : word.toCharArray()) {
            if (c == ' ') {
                return true;
            }
            if (location.equals(Location.CENTER)) {
                return true;
            }
            Location after = location.neighbor(cross);
            if (after.isOnBoard() && isOccupied(after)) {
                return true;
            }
            Location before = location.antineighbor(cross);
            if (before.isOnBoard() && isOccupied(before)) {
                return true;
            }
            location = location.neighbor(direction);
        }
        return false;
    }

    /**
     * Finds the start of a (cross) word including location and moving in direction.
     */
    public Location findStartOfWord(Location location, Location direction) {
        do { // Back up until we leave the board or find an unoccupied square
            location = location.antineighbor(direction);
        } while (location.isOnBoard() && isOccupied(location));
        return location.neighbor(direction);
    }

    /**
     * Returns true if the cross word including (but not necessarily starting with) location forms a valid dictionary
     * word, or no new cross word was formed at this point.
     *
     * @param tile The one tile played in this word.
     */
    public boolean isValidWord(Location location, Location direction, char tile) {
        if (tile == ' ') {
            return true; // Word was already on board
        }
        location = findStartOfWord(location, direction);
        String word = "";
        boolean tileUsed = false;
        while (location.isOnBoard()) {
            if (isOccupied(location)) {
                word += getSquare(location);
            } else if (tileUsed) {
                break;
            } else {
                word += tile;
                tileUsed = true;
            }
            location = location.neighbor(direction);
        }
        if (word.length() == 1) {
            return true;
        }
        return DICTIONARY.contains(word.toLowerCase());
    }

    /**
     * Returns true if word, played at location in direction, forms a valid dictionary word of at least two letters.
     */
    public boolean isValidWord(String word, Location location, Location direction) {
        if (word.length() < 2) {
            return false;
        }
        char[] letters = new char[word.length()];
        for (int i = 0; i < word.length(); i++) {
            if (isOccupied(location)) {
                letters[i] = getSquare(location);
            } else {
                letters[i] = word.charAt(i);
            }
            location = location.neighbor(direction);
        }
        return DICTIONARY.contains(new String(letters).toLowerCase());
    }

    /** Returns true if the square at location contains a tile. */
    public boolean isOccupied(Location location) {
        return Character.isAlphabetic(getSquare(location));
    }

    /** Returns true if word, played at location and direction, would create only legal words. */
    public boolean wouldCreateOnlyLegalWords(String word, Location location, Location direction) {
        if (!isValidWord(word, location, direction)) {
            return false;
        }
        Location cross = direction.opposite();
        for (char c : word.toCharArray()) {
            if (!isValidWord(location, cross, c)) {
                return false;
            }
            location = location.neighbor(direction);
        }
        return true;
    }

    /**
     * Returns the score for the cross word including (but not necessarily starting with) location.
     *
     * @param tile The one tile played in this word.
     */
    private int scoreWord(Location location, Location direction, char tile) {
        int score = 0;
        int multiplier = 1;
        location = findStartOfWord(location, direction);
        if (location.neighbor(direction).isOnBoard() && !isOccupied(location.neighbor(direction))) {
            // One letter "cross word"
            return 0;
        }
        boolean tileUsed = false;
        while (location.isOnBoard()) {
            char square = getSquare(location);
            if (isOccupied(location)) {
                score += TILE_VALUES.get(square);
            } else if (tileUsed) {
                break;
            } else {
                score += TILE_VALUES.get(tile);
                char bonus = getSquare(location);
                if (bonus == DOUBLE_LETTER_SCORE) {
                    score += TILE_VALUES.get(tile);
                } else if (bonus == TRIPLE_LETTER_SCORE) {
                    score += 2 * TILE_VALUES.get(tile);
                } else if (bonus == DOUBLE_WORD_SCORE) {
                    multiplier *= 2;
                } else if (bonus == TRIPLE_WORD_SCORE) {
                    multiplier *= 3;
                }
                tileUsed = true;
            }
            location = location.neighbor(direction);
        }
        return score * multiplier;
    }

    /** Returns the points scored for word, played at location in direction. */
    private int scoreWord(String word, Location location, Location direction) {
        int result = 0;
        int multiplier = 1;
        for (char c : word.toCharArray()) {
            char square = getSquare(location);
            if (c == ' ') {
                result += TILE_VALUES.get(square);
            } else {
                result += TILE_VALUES.get(c);
                if (square == DOUBLE_LETTER_SCORE) {
                    result += TILE_VALUES.get(c);
                } else if (square == TRIPLE_LETTER_SCORE) {
                    result += 2 * TILE_VALUES.get(c);
                } else if (square == DOUBLE_WORD_SCORE) {
                    multiplier *= 2;
                } else if (square == TRIPLE_WORD_SCORE) {
                    multiplier *= 3;
                }
            }
            location = location.neighbor(direction);
        }
        result *= multiplier;
        return result;
    }

    /** Returns the score for playing word at location in direction, including any cross words. */
    public int score(String word, Location location, Location direction) {
        // Score word submitted
        int result = scoreWord(word, location, direction);
        int tilesPlayed = 0;
        // Score cross words
        for (char c : word.toCharArray()) {
            if (c != ' ') {
                result += scoreWord(location, direction.opposite(), c);
                tilesPlayed++;
            }
            location = location.neighbor(direction);
        }
        if (tilesPlayed == 7) {
            result += 50;
        }
        return result;
    }

    /** Throws an IllegalMoveException if playing word at location in direction from hand would not be legal. */
    public void verifyLegality(String word, Location location, Location direction, List<Character> hand) throws IllegalMoveException {
        if (word.length() < 2) {
            throw new IllegalMoveException("Word must be at least two letters long.");
        }
        boolean containsNonspace = false;
        for (char c : word.toCharArray()) {
            if (c != ' ') {
                containsNonspace = true;
                break;
            }
        }
        if (!containsNonspace) {
            throw new IllegalMoveException("Word must contain at least one new tile.");
        }
        if (!canBeDrawnFromHand(word, hand)) {
            throw new IllegalMoveException("Hand does not contain sufficient tiles to play word.");
        }
        if (!canBePlacedOnBoard(word, location, direction)) {
            throw new IllegalMoveException("Board placement incorrect (gaps, overlapping tiles, edge of board).");
        }
        if (!wouldBeConnected(word, location, direction)) {
            throw new IllegalMoveException("Board placement incorrect (gaps, overlapping tiles, edge of board).");
        }
        if (!wouldCreateOnlyLegalWords(word, location, direction)) {
            throw new IllegalMoveException("Invalid word created.");
        }
    }

    /** Removes the tiles used in word from hand and returns them in a new String. */
    public String removeTiles(String word, List<Character> hand) {
        return null;
    }

    /** Returns the current player number (0 or 1). */
    public int getCurrentPlayer() {
        return 1;
    }

    /** Returns true if the game is over. */
    public boolean gameIsOver() {
        return false;
    }

    /** Scores any unplayed tiles at the end of the game. */
    private void scoreUnplayedTiles() {

    }
}
