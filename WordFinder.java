package scrabble;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
        public void getAllWords(char[] input, LinkedList<String> output) {
            if (this.word != null) { output.add(this.word); }
            if (input.length > 1) {
                char[] subInput = new char[input.length - 1];
                for (int i = 0; i < subInput.length; i++) {
                    subInput[i] = input[i + 1];
                }

                // stores letters that we've already checked.
                boolean[] seen = new boolean[26];

                for (int i = 0; i < input.length; i++) {
                    int charIdx = (int)input[i] - 97;
                    if (seen[charIdx] != true) {
                        seen[charIdx] = true;
                        WordNode child = this.getChild(input[i]);
                        if (child != null) {
                            child.getAllWords(subInput, output);
                        }
                    }
                    if (i < subInput.length) { subInput[i] = input[i]; }
                }
            } else {
                WordNode child = this.getChild(input[0]);
                if (child != null && child.word != null ) { output.add(child.word); }
            }

        }

    }
    // END wordnode


    public WordFinder() {
        createWordTree();
    }

    private WordNode root = new WordNode();

    public void createWordTree() {
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

    public boolean validWord(String word) {
        return this.root.validWord(word, 0);
    }

    public void getAvailableWords(char[] hand, LinkedList<String> output) {
        int _count = 0;

        int parseIndex = 0;
        char[] parsedHand = new char[hand.length];
        for (int i = 0; i < hand.length; i++) {
            if (hand[i] == '_') { _count++; } else { parsedHand[parseIndex] = hand[i]; parseIndex++; }
        }

        for (int i = 0; i < _count; i++) { parsedHand[parsedHand.length - 1 - i] = 'e'; }
        this.root.getAllWords(parsedHand, output);
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
        char[] hand = "th__".toCharArray();

        util.getAvailableWords(hand, out);
        long endTime = System.nanoTime();

        for (String word : out) {
            System.out.println(word);
        }

        System.out.println("Search took " + ((double)(endTime - startTime) / 1000000.0) + "s");
        System.out.println("Search found " + out.size() + " words");
    }

}
