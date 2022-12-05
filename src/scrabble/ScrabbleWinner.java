package scrabble;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.Out;
import edu.princeton.cs.algs4.StdOut;

import java.util.*;

/**
 * Improvement over 'dumb' implementation
 * Run via "ScrabbleTournament.java"
 *
 * @author Maxwell S. Freudenburg
 * add your names here
 */
public class ScrabbleWinner implements ScrabbleAI {

    private Stack<Integer> rows;
    private Stack<Integer> cols;
    private Stack<Location[]> anchors;
    //private Stack<Guess> guesses;
    private DictGraph dictionary;
    private ArrayList<Move> moves;
    ArrayList<Character> hand;
    private HashSet<String> alreadyLoggedWords;

    Out log;

    /**
     * When exchanging, always exchange everything.
     */
    private static final boolean[] ALL_TILES = {true, true, true, true, true, true, true};

    /**
     * The GateKeeper through which this Incrementalist-Beating Algo accesses the Board.
     */
    private GateKeeper gateKeeper;

    @Override
    public void setGateKeeper(GateKeeper gateKeeper) {
        this.gateKeeper = gateKeeper;
        Start();
    }

    /**
     * ScrabbleTournament calls THIS function to start each move
     * @return
     */
    @Override
    public ScrabbleMove chooseMove() {
        // Update everything
        rows = new Stack<>();
        cols = new Stack<>();
        anchors = new Stack<>();
        hand = gateKeeper.getHand();
        alreadyLoggedWords = new HashSet<>();

        // log hand
        log.println("Hand: " + hand.toString());

        // Do moves - betterFirstMove if first move, else betterMove.
        if (gateKeeper.getSquare(Location.CENTER) == Board.DOUBLE_WORD_SCORE) {
            // INITIALIZATION //
            Start();
            return betterFirstMove();
        }
        return betterMove();
    }

    // Initialize & populate dictionary. Called once
    private void Start() {
        StdOut.println("START");
        // POPULATE DICTIONARY //
        dictionary = new DictGraph("words.txt");
        log = new Out("log.txt");
    }

    /**
     * NEW "move" function
     * Bigger O(n) but smaller n.
     */
    private ScrabbleMove betterMove() {
        log.println("New Move...");

        // READ BOARD //
        // here, we add all the 'occupied' locations to a stack
        Tile t;

        log.println("\t0 1 2 3 4 5 6 7 8 9 0 1 2 3 4");
        log.print("0\t");
        for (int i = 0; i < Board.WIDTH; i++) log.print(gateKeeper.getSquare(new Location(0, i)) + " ");
        log.println();
        // find all possible anchors
        for (int row = 1; row < Board.WIDTH; row++) {
            log.print(row + "\t " + gateKeeper.getSquare(new Location(row, 0)));
            for (int col = 1; col < Board.WIDTH; col++) {
                t = new Tile(row, col, gateKeeper.getSquare(new Location(row, col)));
                log.print(t.value + " ");
                if (t.value >= 'a' && t.value <= 'z') {
                    char test;

                    // check neighbor above
                    test = gateKeeper.getSquare(t.antineighbor(Location.VERTICAL));
                    if (!(test >= 'a') || !(test <= 'z')) {
                        if (!cols.contains(col)) cols.push(col);
                        anchors.push(new Location[]{t.antineighbor(Location.VERTICAL), Location.VERTICAL});
                    }
                    // check neighbor to left
                    test = gateKeeper.getSquare(t.antineighbor(Location.HORIZONTAL));
                    if (!(test >= 'a') || !(test <= 'z')) {
                        if (!rows.contains(row)) rows.push(row);
                        anchors.push(new Location[]{t.antineighbor(Location.HORIZONTAL), Location.HORIZONTAL});
                    }
                    /*
                    // check neighbor below
                    test = gateKeeper.getSquare(t.neighbor(Location.VERTICAL));
                    if (test >= 'a' && test <= 'z') {
                        if (!cols.contains(col)) cols.push(col);
                        anchors.push(new Location[]{t.neighbor(Location.VERTICAL), Location.VERTICAL});
                    }
                    // check neighbor to right
                    test = gateKeeper.getSquare(t.neighbor(Location.HORIZONTAL));
                    if (test >= 'a' && test <= 'z') {
                        if (!rows.contains(row)) rows.push(row);
                        anchors.push(new Location[]{t.neighbor(Location.HORIZONTAL), Location.HORIZONTAL});
                    }

                     */
                }
            }
            log.println();
        }

        PlayWord bestMove = null;

        moves = new ArrayList<>();
        for (Location[] anchor : anchors) {
            log.println("Anchor: " + anchor[0].getRow() + ", " + anchor[0].getColumn());
            findWords(anchor[0], anchor[1]);
        }

        /*
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
                vMove.score =gateKeeper.score(guess, vMove.location, vMove.direction);
                moves.add(hMove);
                moves.add(vMove);
                i++;
            }
            //StdOut.println("Hey we permutated a tile!");
        }*/
        return send();
    }

    /**
     * STEP 1
     * NEW "move" function
     * Bigger O(n) but smaller n.
     */
    private ScrabbleMove betterFirstMove() {
        moves = new ArrayList<>();
        findWords(Location.CENTER, Location.VERTICAL);
        findWords(Location.CENTER, Location.HORIZONTAL);
        return send();
    }

    /**
     * STEP 2
     * Called for each "anchor" - or upon "Location.CENTER" if 1st move
     * @param location spot from which to generate moves
     */
    private void findWords(Location location, Location direction) {
        int row, col;
        for (int i = -1; i < 7; i++) {
            if (direction == Location.HORIZONTAL) {
                row = location.getRow();
                col = location.getColumn() - i;
            } else {
                row = location.getRow() - i;
                col = location.getColumn();
            }
            if (row < 0 || row >= 15 || col < 0 || col >= 15) continue;
            Location _location = new Location(row, col);
            char letter1 = gateKeeper.getSquare(_location);
            if ('a' <= letter1 && letter1 <= 'z') {
                buildWord("" + letter1,
                        dictionary.getEdge(dictionary.root, letter1),
                        _location,
                        _location,
                        direction,
                        hand);
            } else {
                buildWord("",
                        dictionary.root,
                        _location.neighbor(direction), // "neighbor" to avoid an off-by-1 error
                        _location,
                        direction,
                        hand);
            }
        }
    }

    /**
     * STEP 3
     * Builds a word out starting at startLocation. Adds all found moves to ScrabbleWinner.moves ArrayList.
     *
     * @param subWord       The word so far.
     * @param edge          References an edge in our dictionary graph.
     * @param startLocation We keep track of this for easy Move instantiation
     * @param location      This is incremented with each (recursive!) call. 1 letter at a time, thus one location at a time
     * @param direction     Location.HORIZONTAL or Location.VERTICAL. Kind of unwieldy, but OK
     */
    private void buildWord(String subWord, Edge edge, Location startLocation, Location location, Location direction, ArrayList<Character> hand) {
        //ArrayList<Character> _hand = new ArrayList<>(hand);

        if (!location.isOnBoard() || !startLocation.isOnBoard()) return;
        if (edge.isLeaf) return;
        char letter, nextLetter;

        location = location.neighbor(direction);
        if (!location.neighbor(direction).isOnBoard()) return; // return if we've reached the end of the board
        nextLetter = gateKeeper.getSquare(location);

        if ('a' <= nextLetter && nextLetter <= 'z') {
            boolean letterFound = false;
            for (Edge edgeOut : edge.out) {
                if (edgeOut.letter == nextLetter) {
                    letterFound = true;
                }
            }
            if (!letterFound) return; // return if there's no word to make here

            edge = dictionary.getEdge(edge, nextLetter);
            subWord += "" + " ";

            if (edge.isTerminal) {
                if (subWord.contains(" ") || startLocation.equals(Location.CENTER)) {
                    Move move = new Move((subWord), startLocation, direction);
                    move.score = gateKeeper.score(move.word, move.location, move.direction);
                    moves.add(move);
                    if (!alreadyLoggedWords.contains(move.word)) {
                        alreadyLoggedWords.add(move.word);
                        log.printf("Added word\t%-12s", move.word);
                        if (move.direction == Location.VERTICAL) log.print("DOWN from ");
                        else log.print("ACROSS from ");
                        log.printf("(%d, %d).\t", move.location.getRow(), move.location.getColumn());
                        log.println("(hand is " + hand.toString() + ")");
                    }
                }
                //StdOut.println("Found and added a move! Cause: Matching letter on board was terminal.");
            }
            buildWord((subWord), edge, startLocation, location, direction, hand);

        } else {
            for (Edge edgeOut : edge.out) {
                if (!hand.contains(edgeOut.letter)) continue;
                //log.print("hand: " + _hand);
                //_hand.remove(_hand.indexOf(edgeOut.letter));
                if (edgeOut.isTerminal) {
                    if (subWord.contains(" ") || startLocation.equals(Location.CENTER)) {
                        Move move = new Move((subWord + edgeOut.letter), startLocation, direction);
                        move.score = gateKeeper.score(move.word, move.location, move.direction);
                        moves.add(move);
                        if (!alreadyLoggedWords.contains(move.word)) {
                            alreadyLoggedWords.add(move.word);
                            log.printf("Added word\t%-12s", move.word);
                            if (move.direction == Location.VERTICAL) log.print("DOWN from ");
                            else log.print("ACROSS from ");
                            log.printf("(%d, %d).\t", move.location.getRow(), move.location.getColumn());
                            log.println("(hand is " + hand + ")");
                        }
                    }
                    //StdOut.println("Found and added a move! Cause: Letter from generator was terminal.");
                }
                buildWord((subWord + edgeOut.letter), edgeOut, startLocation, location, direction, hand);
            }
        }
    }

    /**
     * STEP 4
     * Sorts all possible moves, looking for highest-scoring legal word.
     * I'm certain that this one works
     * @return
     */
    private ScrabbleMove send() {
        log.println("Starting send()...");

        // sort moves in descending order
        Collections.sort(moves);

        PlayWord bestMove = null;
        //System.out.printf("ScrabbleWinner: I've found %d moves!\n", moves.size());
        int logQuota = 0;
        for (Move move : moves) {
            try {
                gateKeeper.verifyLegality(move.word, move.location, move.direction);
                bestMove = new PlayWord(move.word, move.location, move.direction);
                log.println("I CAN PLAY \"" + move.word + "\"! Score: " + move.score);
                if (bestMove != null) {
                    log.println("bestMove not null.");
                    log.println("Placing " + move.word + " at (" + move.location.getRow() + ", " + move.location.getColumn() + ")");
                    return bestMove;
                }
                //System.out.println("Found a move! Score: " + move.score);
            } catch (IllegalMoveException e) {
                //log.println(e);
            }

        }

        log.println("bestMove SOMEHOW null. Not playing.");
        return new ExchangeTiles(ALL_TILES);
    }

    /*
    private String[] permutate(char[] dAttack) {
        //StdOut.println("Permutator Online");
        //StdOut.println("[" + String.copyValueOf(dAttack) + "]");
        int length = dAttack.length;
        //StdOut.println("Factorial Calculated (recursively, of course)!");

        HashSet<Character> characters = new HashSet<>();
        for (char c : dAttack) {
            if (characters.contains(c)) continue;
            characters.add(c);
        }
        //StdOut.println(characters.size() + ", " + dAttack.length);
        int maxPermutations = factorial(dAttack.length)/(2*factorial(dAttack.length - characters.size()));
        //StdOut.println(maxPermutations);

        // IT IS IN THIS AREA WHERE SOMETHING GOES WRONG SOMETIMES

        HashSet<String> tried = new HashSet<>();
        Stack<String> words = new Stack<>();
        Stack<String> tempGuesses = new Stack<>();

        int numPermutations = 0;

        long time = System.nanoTime();
        int last = 0;
        while (numPermutations < maxPermutations) {
            // we'll need to keep track of the "pivot" letter (the 1 letter from the board, plus its location)
            int oi = (dAttack.length - 1);
            int j = (int) (Math.random() * oi);
            if (j == last) continue;
            last = j;
            char t = dAttack[j];
            dAttack[j] = dAttack[oi];
            dAttack[oi] = t;
            String perm = new String(dAttack);
            if (!tried.contains(perm)) {
                tried.add(perm);
                numPermutations++;
                words.push(String.copyValueOf(dAttack));
                //if (numPermutations%10==0) StdOut.println("[" + numPermutations + " / " + maxPermutations + "]");
            }
            if (time - 30000000000L > 0) break;
        }

        //StdOut.println("Did some permutations");

        HashSet<String> uniqueWords = new HashSet<>();

        int numGuesses = 0;
        while (!words.isEmpty()) {
            String word = words.pop();
            //StdOut.println(word);
            for (int wordLength = 2; wordLength <= length; wordLength++) {
                String guess = word.substring(0, wordLength);
                if (dictionary.contains(guess)) {
                    if (!uniqueWords.contains(guess)) {
                        tempGuesses.push(guess);
                        uniqueWords.add(guess);
                        numGuesses++;
                        //StdOut.println("Found a unique valid word!");
                    }
                }
            }
        }

        String[] guesses = new String[numGuesses];
        int i = 0;
        for (String guess : tempGuesses) {
            //StdOut.println(guess);
            guesses[i++] = guess;
        }

        return guesses;
    }

    static int factorial(int n) {
        if (n == 0)
            return 1;
        else
            return (n * factorial(n - 1));
    }

    private class Guess {
        public Location location;
        public Location direction;
        public char[] tiles;
        public String word;

        //public int letterZero; // 1st index of our Move
        public Guess(Location location, char[] board) {
            this.location = location;
            tiles = new char[15];
            for (int i = 0; i < board.length; i++) {
                tiles[i] = board[i];
            }
            word = "";
        }
    }*/

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

    /**
     * My graph implementation
     */
    public static class DictGraph {
        private final ArrayList<Edge> edges;
        public Edge root;

        /**
         * Constructor. Takes filename as string and does the rest
         * @param filename
         */
        DictGraph(String filename) {
            edges = new ArrayList<>();
            root = new Edge('\0');
            edges.add(root);
            // Collections.addAll(dictionary, in.readAllLines());
            In in = new In(filename);
            for (String word : in.readAllStrings()) {
                AddWord(word);
            }
        }

        /**
         * Go through each char in word. Link 1st char to Edge "root," and make last char terminal
         *
         * @param word word to add to dictionary
         */
        void AddWord(String word) {
            Edge currEdge = null;
            for (int i = -1; i < word.length(); i++) {
                int j = i + 1;

                if (i == -1) {
                    currEdge = addEdge(root, word.charAt(j), false);
                    edges.add(currEdge);
                } else if (j == word.length()) {
                    currEdge.isTerminal = true;
                } else {
                    currEdge = addEdge(currEdge, word.charAt(j), false);
                    edges.add(currEdge);
                }
            }
        }

        /**
         * "Get" the edge from start edge to out edge with char c
         * @param start leading edge
         * @param c following edge
         * @return
         */
        Edge getEdge(Edge start, char c) {
            for (Edge edge : start.out) {
                if (edge.letter == c) return edge;
            }
            return null;
        }

        /**
         * Only used by constructor
         * @param start
         * @param c
         * @param isTerminal
         * @return
         */
        Edge addEdge(Edge start, char c, boolean isTerminal) {
            Edge end = new Edge(c);
            if (isTerminal) end.isTerminal = true;
            start.out.add(end);
            return end;
        }

        // Test Function
        void printTest(int numWordsToPrint) {
            int i = 0;
            for (Edge edgeOut : root.out) {
                if (i >= numWordsToPrint) return;
                System.out.println(edgeOut.letter + makeWord("", edgeOut));
                i++;
            }
        }

        // Test function
        String makeWord(String string, Edge edge) {
            for (Edge edgeOut : edge.out) {
                string += edgeOut.letter;
                if (edgeOut.isTerminal) return string;
                return makeWord(string, edgeOut);
            }
            return null;
        }
    }

    /**
     * An Edge in the graph
     * isTerminal is set to FALSE by default!
     */
    public static class Edge {
        public LinkedList<Edge> out;
        public char letter;
        public boolean isTerminal;
        public boolean isLeaf;

        public Edge(char c) {
            out = new LinkedList<>();
            letter = c;
            isTerminal = false;
        }
    }

}
