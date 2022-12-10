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
         */
        public void getAllWords(char[] hand, char[] fixed, LinkedList<String> output, int depth) {
            if (this.word != null) { output.add(this.word); }
            //System.out.println(String.valueOf(charOptions));

            if (hand.length > 1) {
                char[] subInput = new char[hand.length - 1];
                for (int i = 0; i < subInput.length; i++) {
                    subInput[i] = hand[i + 1];
                }

                // stores letters that we've already checked.
                boolean[] seen = new boolean[26];

                for (int i = 0; i < hand.length; i++) {

                    // get numeric index for currently used char
                    int charIdx = (int)hand[i] - 97;
                    if (seen[charIdx] != true) {

                        seen[charIdx] = true;
                        WordNode child = this.getChild(hand[i]);
                        if (child != null) {
                            child.getAllWords(hand, fixed, output, depth + 1);
                        }
                    }
                    if (i < subInput.length) { subInput[i] = hand[i]; }
                }
            } else {
                WordNode child = this.getChild(hand[0]);
                if (child != null && child.word != null ) {
                    output.add(child.word);
                }
            }

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

    /**
     * Finds all valid words constructable from a given set of letters, with no constraints.
     * @param hand Set of letters to permute
     * @param output Output to write to
     */
    public void getAvailableWords(char[] hand, LinkedList<String> output) {
        int[] letterCount = new int[26];
        for (int i = 0; i < hand.length; i++) {
            int charIdx = (int)hand[i] - 97;
            letterCount[charIdx]++;
        }

        ArrayList<char[]> charOptions = new ArrayList<char[]>();
        for (int i = 0; i < hand.length; i++) {
            charOptions.add(hand);
        }

        boolean[] fixed = new boolean[charOptions.size()];

        //this.getAvailableWords(charOptions, letterCount, fixed, output);
    }

    /**
     * !!!!THIS METHOD CURRENTLY HAS BUGS! USE THE ONE ABOVE FOR NOW!
     * Finds all valid words given a list of options for each character.
     * @param output Output list to write to.
     */
    public void getAvailableWords(char[] hand, char[] fixed, LinkedList<String> output) {
        this.root.getAllWords(hand, fixed, output, 0);
    }

    public static void main(String[] args) {
        WordFinder util = new WordFinder();

//        System.out.println(util.validWord("yes"));
//        System.out.println(util.validWord("zoophytic"));
//        System.out.println(util.validWord("zygomatic"));
//        System.out.println(util.validWord("maxilla"));
//        System.out.println(util.validWord("unquestionably"));
//        System.out.println(util.validWord("sdigbakjd"));

        long startTime = System.nanoTime();
        LinkedList<String> out = new LinkedList<String>();
        //char[] hand = "bcdfghjklmnpqrstvwx".toCharArray();
        //char[] hand = { 'a', 'e', 'i', 'o', 'u', 't', 'r', 'r', 'n', 'r', 'r' };
        //char[] hand = { 'x', 'q', 'g', 'f', 'r', 't', 'k', 'm', 'f', 'r', 'l' };
        //char[] hand = "th__".toCharArray();
        char[] hand = "aieepthr".toCharArray();
        char[] fixed = new char[hand.length];
        util.getAvailableWords(hand, fixed, out);

        long endTime = System.nanoTime();

        for (String word : out) {
            System.out.println(word);
        }

        System.out.println("Search took " + ((double)(endTime - startTime) / 1000000.0) + "s");
        System.out.println("Search found " + out.size() + " words");
    }

}
