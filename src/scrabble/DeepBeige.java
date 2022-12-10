package scrabble;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.Out;
import edu.princeton.cs.algs4.Queue;
import edu.princeton.cs.algs4.StdOut;

import java.util.*;

/**
 * Improvement over 'dumb' implementation
 * @author Maxwell S. Freudenburg
 * add your names here
 */
public class DeepBeige implements ScrabbleAI {

    private Stack<Tile> tiles;
    private Stack<Location> anchors;
    private ArrayList<Move> moves;
    private WordFinder wordfinder;

    /** When exchanging, always exchange everything. */
    private static final boolean[] ALL_TILES = {true, true, true, true, true, true, true};

    /** The GateKeeper through which this Incrementalist-Beating Algo accesses the Board. */
    private GateKeeper gateKeeper;

    @Override
    public void setGateKeeper(GateKeeper gateKeeper) {
        this.gateKeeper = gateKeeper;
    }

    @Override
    public ScrabbleMove chooseMove() {
        if (tiles == null) tiles = new Stack<>();
        // READ BOARD //
        // here, we add all the 'occupied' locations to a stack
        Tile t;
        for (int i = 0; i < Board.WIDTH; i++) {
            for (int j = 0; j < Board.WIDTH; j++) {
                t = new Tile(i, j, gateKeeper.getSquare(new Location(i,j)));
                if (t.value >= 'a' && t.value <= 'z')
                    tiles.push(t);
            }
        }

        if (gateKeeper.getSquare(Location.CENTER) == Board.DOUBLE_WORD_SCORE) {
            // INITIALIZATION //
            return betterFirstMove();
        }
        return betterMove();
    }

    public DeepBeige() {
        tiles = new Stack<>();
        wordfinder = new WordFinder();
        moves = new ArrayList<>();
    }

    /** NEW "move" function
     * Bigger O(n) but smaller n.
     * */
    private ScrabbleMove betterMove() {
        ArrayList<Character> hand = gateKeeper.getHand();
        //Stack<Location> anchors = new Stack<>();
        PlayWord bestMove = null;
        //int bestScore = -1;

        StdOut.println("Starting BetterMove");
        moves = new ArrayList<>();

        /** Find & test words (I'm using Fischer-Yates shuffle)
         * Add possible moves to 'moves' ArrayList
         */
        for (Tile tile : tiles) {
            //StdOut.println("Starting On a Tile");

            // dAttack[] is our 'string'
            char[] dAttack = new char[hand.size()+1];
            for (int i = 0; i < hand.size(); i++) {
                dAttack[i] = hand.get(i);
            }
            dAttack[hand.size()] = tile.value;

            //StdOut.println("Permutating...");
            String[] guesses = permutate(dAttack);

            for (int i = 0; i < guesses.length; i++) {
                //StdOut.println("Trying a guess");
                String guess = guesses[i];
                Move hMove = new Move(guess, tile.antineighbor(Location.HORIZONTAL), Location.HORIZONTAL);
                Move vMove = new Move(guess, tile.antineighbor(Location.VERTICAL), Location.VERTICAL);
                hMove.score = gateKeeper.score(guess, hMove.location, hMove.direction);
                vMove.score = gateKeeper.score(guess, vMove.location, vMove.direction);
                moves.add(hMove);
                moves.add(vMove);
                i++;
            }
            //StdOut.println("Hey we permutated a tile!");
        }
        return send();
    }

    /** NEW "move" function
     * Bigger O(n) but smaller n.
     * */
    private ScrabbleMove betterFirstMove() {
        ArrayList<Character> hand = gateKeeper.getHand();

        char[] dAttack = new char[hand.size()];
        for (int i = 0; i < hand.size(); i++) {
            dAttack[i] = hand.get(i);
        }

        String[] guesses = permutate(dAttack);

        for (int i = 0; i < guesses.length; i++){
            String guess = guesses[i];
            Move hMove = new Move(guess, Location.CENTER.antineighbor(Location.HORIZONTAL), Location.HORIZONTAL);
            Move vMove = new Move(guess, Location.CENTER.antineighbor(Location.VERTICAL), Location.VERTICAL);
            hMove.score = gateKeeper.score(guess, hMove.location, hMove.direction);
            vMove.score = gateKeeper.score(guess, vMove.location, vMove.direction);
            moves.add(hMove);
            moves.add(vMove);
            i++;
        }

        return send();
    }

    private ScrabbleMove send() {

        // sort moves in descending order
        Collections.sort(moves);

        PlayWord bestMove = null;

        for (Move move : moves) {
            try {
                gateKeeper.verifyLegality(move.word, move.location, move.direction);
                bestMove = move.move;
                System.out.println("Found a move! Score: " + move.score);
            } catch (IllegalMoveException e) {
                // It wasn't legal; go on to the next one
            }

        }
        if (bestMove != null) {
            return bestMove;
        }
        return new ExchangeTiles(ALL_TILES);
    }

    private String[] permutate(char[] dAttack) {
        LinkedList<String> output = new LinkedList<String>();
        wordfinder.getAvailableWords(dAttack, output);

        String[] ret = new String[output.size()];
        output.toArray(ret);

        return ret;
    }

    /** This is necessary for the first turn, as one-letter words are not allowed. */
    private ScrabbleMove findTwoTileMove() {
        ArrayList<Character> hand = gateKeeper.getHand();
        String bestWord = null;
        int bestScore = -1;
        for (int i = 0; i < hand.size(); i++) {
            for (int j = 0; j < hand.size(); j++) {
                if (i != j) {
                    try {
                        char a = hand.get(i);
                        if (a == '_') {
                            a = 'E'; // This could be improved slightly by trying all possibilities for the blank
                        }
                        char b = hand.get(j);
                        if (b == '_') {
                            b = 'E'; // This could be improved slightly by trying all possibilities for the blank
                        }
                        String word = "" + a + b;
                        gateKeeper.verifyLegality(word, Location.CENTER, Location.HORIZONTAL);
                        int score = gateKeeper.score(word, Location.CENTER, Location.HORIZONTAL);
                        if (score > bestScore) {
                            bestScore = score;
                            bestWord = word;
                        }
                    } catch (IllegalMoveException e) {
                        // It wasn't legal; go on to the next one
                    }
                }
            }
        }
        if (bestScore > -1) {
            return new PlayWord(bestWord, Location.CENTER, Location.HORIZONTAL);
        }
        return new ExchangeTiles(ALL_TILES);
    }

    /**
     * Technically this tries to make a two-letter word by playing one tile; it won't find words that simply add a
     * tile to the end of an existing word.
     */
    private ScrabbleMove findOneTileMove() {
        ArrayList<Character> hand = gateKeeper.getHand();
        PlayWord bestMove = null;
        int bestScore = -1;
        for (char c : hand) {
            if (c == '_') {
                c = 'E'; // This could be improved slightly by trying all possibilities for the blank
            }
            for (String word : new String[]{c + " ", " " + c}) {
                for (int row = 0; row < Board.WIDTH; row++) {
                    for (int col = 0; col < Board.WIDTH; col++) {
                        Location location = new Location(row, col);
                        for (Location direction : new Location[]{Location.HORIZONTAL, Location.VERTICAL}) {
                            try {
                                gateKeeper.verifyLegality(word, location, direction);
                                int score = gateKeeper.score(word, location, direction);
                                if (score > bestScore) {
                                    bestScore = score;
                                    bestMove = new PlayWord(word, location, direction);
                                }
                            } catch (IllegalMoveException e) {
                                // It wasn't legal; go on to the next one
                            }
                        }
                    }
                }
            }
        }
        if (bestMove != null) {
            return bestMove;
        }
        return new ExchangeTiles(ALL_TILES);
    }

    static int factorial(int n) {
        if (n == 0)
            return 1;
        else
            return (n * factorial(n - 1));
    }

    /**
     * Almost the same as 'Location' class, but contains Location's char
     * for convenience.
     */
    private class Tile extends Location {
        // letting 'value' be public, because 'Tile' is private
        public char value;
        public Tile(int row, int column, char c) {
            super(row, column);
            value = c;
        }
    }

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
            Move other = (Move) o;
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

}
