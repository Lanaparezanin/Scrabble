package scrabble;

import edu.princeton.cs.algs4.LinkedQueue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;

public class WordFinder {

    private class WordNode {
        private String word = null;
        private WordNode[] children = new WordNode[26];

        // constructors
        public WordNode() {}

        public WordNode(String word) {
            this.word = word;
        }
        // END constructors

        public String getWord() {
            return this.word;
        }

        public void addChild(char index, WordNode child) {
            // convert to array index, 'a' is char value 97, so it maps to 0
            int intDex = (int)index - 97;
            this.children[intDex] = child;
        }

        public WordNode getChild(char index) {
            // convert to array index, 'a' is char value 97, so it maps to 0
            int intDex = (int)index - 97;
            return this.children[intDex];
        }

        /**
         * Gets the child at the given index, or if it doesn't exist, creates a blank one.
         */
        private WordNode getChildW(char index) {
            // convert to array index, 'a' is char value 97, so it maps to 0
            int intDex = (int)index - 97;
            if (this.children[intDex] == null) { this.children[intDex] = new WordNode(); }
            return this.children[intDex];
        }

        public void addWord(String word) {
            if (word.length() == 1) { this.word = word; } else {
                getChildW(word.charAt(0)).addWordRecursive(word, 1);
            }
        }

        private void addWordRecursive(String word, int depth) {
            if (word.length() == depth) { this.word = word; } else {
                getChildW(word.charAt(depth)).addWordRecursive(word, depth + 1);
            }
        }

        public boolean validWord(String word, int depth) {
            if (word.length() == depth) { return true; } else {
                WordNode child = getChild(word.charAt(depth));
                if (child != null) { return child.validWord(word, depth + 1); } else { return false; }
            }
        }

        /**
         * If this node has a word in it, adds it to the output, and then tries each permutation of the input.
         *
         * Anchor is a letter in the hand that is required to be in the word.
         * passedAnchor stores if we have passed the anchor letter and can add this section of the tree to output.
         * wordVal is a char[] that builds the return values for output.
         */
        public void getAllWords(char[] hand, char anchor, ArrayList<String> output, int depth, boolean passedAnchor, char[] wordVal) {
            if (this.word != null && passedAnchor) {
                output.add(new String(wordVal, 0, depth));
            }

            if (hand.length > 1) {
                char[] subInput = new char[hand.length - 1];
                for (int i = 0; i < subInput.length; i++) {
                    subInput[i] = hand[i + 1];
                }

                // stores letters that we've already checked.
                boolean[] seen = new boolean[26];

                for (int i = 0; i < hand.length; i++) {

                    // if tile is wild, check all letters. Wilds are only used after all other letters are exhausted.
                    if (hand[i] == '_') {
                        for (char c = 'a'; c < 'z'; c++) {
                            int charIdx = (int)c - 97;
                            if (seen[charIdx] != true) {
                                seen[charIdx] = true;
                                WordNode child = this.getChild(c);
                                if (child != null) {
                                    // using a wild guarantees that this was not the anchor tile
                                    wordVal[depth] = Character.toUpperCase(c);
                                    child.getAllWords(subInput, anchor, output, depth + 1, passedAnchor, wordVal);
                                }
                            }
                        }
                    } else {
                        // normal behavior
                        int charIdx = ((int)hand[i]) - 97;
                        if (seen[charIdx] != true) {

                            seen[charIdx] = true;
                            WordNode child = this.getChild(hand[i]);
                            if (child != null) {
                                boolean passingAnchor = (passedAnchor || hand[i] == anchor);
                                wordVal[depth] = hand[i];
                                child.getAllWords(subInput, anchor, output, depth + 1, passingAnchor, wordVal);
                            }
                        }
                    }
                    if (i < subInput.length) { subInput[i] = hand[i]; }
                }

            } else {
                // hand only has 1 character left.

                // character is wild
                if (hand[0] == '_') {
                    // wilds can never be the anchor.
                    if (passedAnchor) {
                        for (char c = 'a'; c < 'z'; c++) {
                            WordNode child = this.getChild(c);
                            if (child != null && child.word != null) {
                                wordVal[depth] = Character.toUpperCase(c);
                                output.add(new String(wordVal, 0, depth));
                            }
                        }
                    }
                } else {
                    // character is a normal tile
                    WordNode child = this.getChild(hand[0]);
                    boolean passingAnchor = (passedAnchor || hand[0] == anchor);
                    if (passingAnchor && child != null && child.word != null) {
                        wordVal[depth] = hand[0];
                        output.add(new String(wordVal, 0, depth));
                    }
                }
            }
            wordVal[depth] = 0;

        }

    }
    // END wordnode


    // START class variables
    private WordNode root = new WordNode();
    // END class variables


    // START constructor
    public WordFinder() {
        File data = new File("./resources/words.txt");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(data));

            while (reader.ready()) {
                root.addWord(reader.readLine());
            }

        } catch (Exception e) {
            System.out.println("Couldn't find wordlist");
        }
    }
    // END constructor


    // START methods

    /**
     * Checks if a given string is a valid word
     */
    public boolean validWord(String word) {
        return this.root.validWord(word, 0);
    }


    private char[] parseHand(char[] hand) {
        char[] parsedHand = new char[hand.length];

        int foundBlanks = 0;
        int currentIdx = 0;
        for (int i = 0; i < hand.length; i++) {
            if (hand[i] == '_') {
                foundBlanks++;
            } else {
                parsedHand[currentIdx] = hand[i];
                currentIdx++;
            }
        }

        // moves blanks to the end of the hand.
        for (int i = 0; i < foundBlanks; i++) {
            parsedHand[parsedHand.length - 1 - i] = '_';
        }

        return parsedHand;
    }


    public void getAvailableWords(char[] hand, ArrayList<String> output) {
        this.root.getAllWords(parseHand(hand), '_', output, 0, true, new char[100]);
    }


    /**
     * Finds all valid words constructable from a given set of letters, with no constraints.
     * @param hand Set of letters to permute
     * @param output Output to write to
     */
    public void getAvailableWords(char[] hand, char anchor, ArrayList<String> output) {
        this.root.getAllWords(parseHand(hand), anchor, output, 0, false, new char[100]);
    }

    public static void main(String[] args) {
        WordFinder util = new WordFinder();

        long startTime = System.currentTimeMillis();
        ArrayList<String> out = new ArrayList<String>();
        char[] hand = "peeem__t".toCharArray();
        char[] fixed = new char[hand.length];
        util.getAvailableWords(hand, 't', out);

        long endTime = System.currentTimeMillis();

        for (String word : out) {
            System.out.println(word);
        }

        System.out.println("Search took " + (endTime - startTime) + "ms");
        System.out.println("Search found " + out.size() + " words");
    }

}
