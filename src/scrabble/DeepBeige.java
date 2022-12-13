package scrabble;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Stack;

public class DeepBeige implements ScrabbleAI {

    /**
     * When exchanging, always exchange everything.
     */
    private static final boolean[] ALL_TILES = {true, true, true, true, true, true, true};

    private GateKeeper gateKeeper;
    private WordFinder wordFinder;
    private PossiBoard currentBoard;

    private final boolean DEBUG = false;

    private static int[] FULL_BAG = new int[27];
    private static final int TOTAL_TILES = "aaaaaaaaabbccddddeeeeeeeeeeeeffggghhiiiiiiiiijkllllmmnnnnnnooooooooppqrrrrrrssssttttttuuuuvvwwxyyz__".length();

    static {
        char[] bagChars = "aaaaaaaaabbccddddeeeeeeeeeeeeffggghhiiiiiiiiijkllllmmnnnnnnooooooooppqrrrrrrssssttttttuuuuvvwwxyyz__".toCharArray();
        for (int i = 0; i < bagChars.length; i++) {
            if (bagChars[i] == '_') {
                FULL_BAG[26]++;
            } else {
                int charIdx = ((int)bagChars[i]) - 97;
                FULL_BAG[charIdx]++;
            }
        }
    }


    // END local vars, START constructor


    public DeepBeige() {
        wordFinder = new WordFinder();
        currentBoard = new PossiBoard();

        testHand.add('a');
        testHand.add('e');
        testHand.add('i');
        testHand.add('o');
        testHand.add('u');
        testHand.add('r');
        testHand.add('t');
        testHand.add('s');
        testHand.add('l');
    }


    // END constructor, START local classes


    /**
     * Score-sortable encapsulation for Scrabble 'PlayWord' class
     */
    private class Move implements Comparable {
        public PlayWord move;
        public int score;

        public String word;
        public Location location;
        public Location direction;

        public Move(String word, Location location, Location direction) {
            this.word = word;
            this.location = location;
            this.direction = direction;
        }

        @Override
        public int compareTo(Object o) {
            DeepBeige.Move other = (DeepBeige.Move) o;
            // this is actually BACKWARDS, as we want the list of moves to be in DESCENDING order.
            if (this.score < other.score) {
                return 1;
            } else if (this.score == other.score) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    private class LocationPlus extends Location {

        public int x;
        public int y;

        public LocationPlus(int row, int column) {
            super(row, column);
            this.x = row;
            this.y = column;
        }
    }


    // END local classes, START class methods


    @Override
    public void setGateKeeper(GateKeeper gateKeeper) {
        this.gateKeeper = gateKeeper;
    }

    @Override
    public ScrabbleMove chooseMove() {
        ScrabbleMove move;

        if (gateKeeper.getSquare(Location.CENTER) == Board.DOUBLE_WORD_SCORE) {
            move = getFirstMove();
        } else {
            move = searchBestMove();
//            ArrayList<Character> _hand = this.gateKeeper.getHand();
//            char[][] boardLayout = new char[Board.WIDTH][Board.WIDTH];
//            move = getTopScoreMove(_hand, gateKeeper, boardLayout);
//            currentBoard = new PossiBoard(boardLayout, move, true);
//            System.out.println(currentBoard.value);
        }
        return move;
    }

    private static ArrayList<Character> testHand = new ArrayList<>();


    private ScrabbleMove searchBestMove() {

        int unknownTiles = TOTAL_TILES;
        int[] knownBag = new int[27];
        for (int i = 0; i < knownBag.length; i++) {
            knownBag[i] = FULL_BAG[i];
        }
        ArrayList<Character> _hand = this.gateKeeper.getHand();
        for (Character c : _hand) {
            if (Character.isAlphabetic(c)) {
                if (Character.isUpperCase(c)) {
                    knownBag[26]--;
                    unknownTiles--;
                } else {
                    int charIdx = ((int)c) - 97;
                    knownBag[charIdx]--;
                    unknownTiles--;
                }
            }
        }

        LinkedList<ScrabbleMove> legalMoves =  getAllMoves(_hand, this.gateKeeper);
        // get current board
        char[][] boardLayout = new char[Board.WIDTH][Board.WIDTH];
        for (int row = 0; row < Board.WIDTH; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {
                char c = this.gateKeeper.getSquare(new Location(row, col));
                boardLayout[row][col] = c;
                if (Character.isAlphabetic(c)) {
                    if (Character.isUpperCase(c)) {
                        knownBag[26]--;
                        unknownTiles--;
                    } else {
                        int charIdx = ((int)c) - 97;
                        knownBag[charIdx]--;
                        unknownTiles--;
                    }
                }
            }
            //System.out.println("");
        }

        ArrayList<Character> opponent_hand = new ArrayList<Character>();
        if (unknownTiles <= 14) {
            // deep copy the known bag so we have a probabilistic one to pull from
            int[] probabilisticBag = new int[27];
            for (int i = 0; i < knownBag.length; i++) {
                probabilisticBag[i] = knownBag[i];
            }

            // find the 7 most common letters
            boolean tilesRemain = true;
            for (int i = 0; i < 6; i++) {
                int maxIndex = 0;
                for (int j = 1; j < probabilisticBag.length; j++) {
                    if (probabilisticBag[maxIndex] < probabilisticBag[j]) {
                        maxIndex = j;
                    }
                }

                if (knownBag[maxIndex] > 0) {
                    if (maxIndex == 26) {
                        opponent_hand.add('_');
                    } else {
                        opponent_hand.add((char) (maxIndex + 97));
                        knownBag[maxIndex]--;
                    }
                } else {
                    tilesRemain = false;
                    break;
                }
                probabilisticBag[maxIndex] -= 5;
            }
        } else {
            opponent_hand = testHand;
        }

        int maxScore = -10000;
        ScrabbleMove bestMove = new ExchangeTiles(ALL_TILES);

        System.out.println("Guessing opponent's hand is: ");
        for (char c : opponent_hand) {
            System.out.print(c + " ");
        }
        System.out.println("");

        //System.out.println("Searching " + legalMoves.size() + " moves");
        for (ScrabbleMove move : legalMoves) {
            PossiBoard p = new PossiBoard(boardLayout, move, true);
            //System.out.println(p.toString());
            if (boardValue(p, false, opponent_hand, 0) > maxScore) {
                maxScore = p.value;
                bestMove = move;
                //System.out.println("new best:" + p.value);
            }
        }

        return bestMove;
    }

    // player true if it's us
    private int boardValue(PossiBoard board, boolean player, ArrayList<Character> hand, int depth) {
        if (depth >= 1) { return board.value; }

        // gatekeeper's player doesn't matter.
        LinkedList<ScrabbleMove> legalMoves = getAllMoves(hand, new GateKeeper(board, 0));
        int maxScore = -10000;
        for (ScrabbleMove move : legalMoves) {
            PossiBoard p = new PossiBoard(board, move, player);
            if (boardValue(p, !player, hand,depth + 1) > maxScore) { maxScore = p.value; }
        }

        return maxScore;
    }


    /**
     * Gets a list of available moves, sorted by score.
     */
    private LinkedList<ScrabbleMove> getAllMoves(ArrayList<Character> _hand, GateKeeper gateKeeper) {
        long startTime;
        if (DEBUG) {startTime = System.currentTimeMillis();}

        // find open spots for moves
        Stack<LocationPlus> possibleMoves = new Stack<LocationPlus>();
        for (int row = 0; row < Board.WIDTH; row++) {
            for (int col = 0; col < Board.WIDTH; col++) {
                LocationPlus loc = new LocationPlus(row, col);
                char space = gateKeeper.getSquare(loc);

                if (Character.isAlphabetic(space)) {
                    boolean free = false;
                    Location n = loc.neighbor(Location.HORIZONTAL);
                    if (n.isOnBoard() && !Character.isAlphabetic(gateKeeper.getSquare(n))) { free = true; }
                    n = loc.antineighbor(Location.HORIZONTAL);
                    if (n.isOnBoard() && !Character.isAlphabetic(gateKeeper.getSquare(n))) { free = true; }
                    n = loc.neighbor(Location.VERTICAL);
                    if (n.isOnBoard() && !Character.isAlphabetic(gateKeeper.getSquare(n))) { free = true; }
                    n = loc.antineighbor(Location.VERTICAL);
                    if (n.isOnBoard() && !Character.isAlphabetic(gateKeeper.getSquare(n))) { free = true; }
                    if (free) {
                        possibleMoves.push(loc);
                    }
                }
            }
        }


        // initialize storage for highest-scoring move
        int maxScore = -1;
        LinkedList<ScrabbleMove> moves = new LinkedList<ScrabbleMove>();

        while (!possibleMoves.empty()) {
            LocationPlus location = possibleMoves.pop();
            char anchor = Character.toLowerCase(gateKeeper.getSquare(location));

            // get all permutations of hand + anchor.
            char[] hand = new char[_hand.size() + 1];
            for (int i = 0; i < _hand.size(); i++) {
                hand[i] = _hand.get(i);
            }
            hand[hand.length - 1] = anchor;

            ArrayList<String> out = new ArrayList<String>();
            wordFinder.getAvailableWords(hand, anchor, out);

            for (String _word : out) {

                // get location that this word would be played at.
                // TODO ignores duplicates of the anchor, missing potential words.
                int aIdx = _word.indexOf(anchor);
                LocationPlus vLocation = new LocationPlus(location.x - aIdx, location.y);
                LocationPlus hLocation = new LocationPlus(location.x, location.y - aIdx);

                // convert to a word with a space in it, this is the string we give to PlayMove
                char[] tmp = new char[_word.length()];
                for (int i = 0; i < tmp.length; i++) {
                    if (i == aIdx) { tmp[i] = ' '; } else { tmp[i] = _word.charAt(i); }
                }
                _word = String.valueOf(tmp);

                // try horizontal
                try {
                    gateKeeper.verifyLegality(_word, hLocation, Location.HORIZONTAL);
                    int score = gateKeeper.score(_word, hLocation, Location.HORIZONTAL);
                    //if (maxScore < score) {
                    if (true) {
                        //System.out.println("added " + _word);
                        moves.add(new PlayWord(_word, hLocation, Location.HORIZONTAL));
                        maxScore = score;
                    }
                } catch (Exception e) {
                    //System.out.print(_word + " with h-anchor " + anchor + " at " + hLocation.x + ", " + hLocation.y + " was invalid: ");
                    //System.out.println(e.getMessage());
                }

                // try vertical
                try {
                    gateKeeper.verifyLegality(_word, vLocation, Location.VERTICAL);
                    int score = gateKeeper.score(_word, vLocation, Location.VERTICAL);
                    //if (maxScore < score) {
                    if (true) {
                        //System.out.println("added " + _word);
                        moves.add(new PlayWord(_word, vLocation, Location.VERTICAL));
                        maxScore = score;
                    }
                } catch (Exception e) {
                    //System.out.print(_word + " with v-anchor " + anchor + " at " + vLocation.x + ", " + vLocation.y + " was invalid: ");
                    //System.out.println(e.getCause());
                }
            }
        }

        return moves;
    }


    /**
     * Runs if this is the first move of the game
     */
    private ScrabbleMove getFirstMove() {
        ArrayList<Character> _hand = this.gateKeeper.getHand();
        char[] hand = new char[_hand.size()];
        for (int i = 0; i < _hand.size(); i++) {
            hand[i] = _hand.get(i);
        }

        ArrayList<String> out = new ArrayList<String>();
        wordFinder.getAvailableWords(hand, out);

        int maxScore = -1;
        String word = "";
        Location loc = Location.CENTER;
        Location rot = Location.HORIZONTAL;
        for (String _word : out) {
            try {
                int score = gateKeeper.score(_word, Location.CENTER, Location.HORIZONTAL);
                if (maxScore < score) {
                    word = _word;
                    maxScore = score;
                }
            } catch (Exception e) {}

        }

        if (DEBUG) {
            System.out.println(word);
            System.out.println(String.valueOf(hand));
        }
        return new PlayWord(word, Location.CENTER, Location.HORIZONTAL);
    }
}