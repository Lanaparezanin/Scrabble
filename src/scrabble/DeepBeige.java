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

    private final boolean DEBUG = false;


    // END local vars, START constructor


    public DeepBeige() {
        wordFinder = new WordFinder();
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
        if (gateKeeper.getSquare(Location.CENTER) == Board.DOUBLE_WORD_SCORE) { return getFirstMove(); }

        ArrayList<Character> _hand = this.gateKeeper.getHand();
        return getTopScoreMove(_hand);
    }

    /**
     * Given a hand, returns the highest-scoring move.
     * @return
     */
    private ScrabbleMove getTopScoreMove(ArrayList<Character> _hand) {

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
        String word = "";
        LocationPlus playLocation = new LocationPlus(7, 7);
        Location playRotation = Location.HORIZONTAL;

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
                System.out.println(_word);

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
                    if (maxScore < score) {
                        word = _word;
                        playLocation = new LocationPlus(hLocation.x, hLocation.y);
                        playRotation = Location.HORIZONTAL;
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
                    if (maxScore < score) {
                        word = _word;
                        playLocation = new LocationPlus(vLocation.x, vLocation.y);
                        playRotation = Location.VERTICAL;
                        maxScore = score;
                    }
                } catch (Exception e) {
                    //System.out.print(_word + " with v-anchor " + anchor + " at " + vLocation.x + ", " + vLocation.y + " was invalid: ");
                    //System.out.println(e.getCause());
                }
            }
        }

        if (DEBUG) {
            long endTime = System.currentTimeMillis();
            System.out.println("Move took " + (endTime - startTime) + "ms");
            System.out.println("Hand was " + String.valueOf(_hand));
        }

        if (word.length() == 0) {

            if (DEBUG) {System.out.println("exchanged hand");}
            return new ExchangeTiles(ALL_TILES);
        }

        if (DEBUG) {System.out.println("Scored " + maxScore + " by playing " + word + " at " + playLocation.x + ", " + playLocation.y);}
        return new PlayWord(word, playLocation, playRotation);
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