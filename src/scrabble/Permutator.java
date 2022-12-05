package scrabble;

import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.Out;
import edu.princeton.cs.algs4.StdOut;

import java.util.HashSet;
import java.util.Stack;

/**
 * A script to generate an exhaustive list of all possible permutations of a given set of characters
 *
 * @author Maxwell S. Freudenburg
 */
public class Permutator {

    static int factorial(int n) {
        if (n == 0)
            return 1;
        else
            return (n * factorial(n - 1));
    }

    public static void main(String[] args) {

        int length = 8;
        int maxPermutations;

        HashSet<String> dictionary = new HashSet<>();
        HashSet<String> tried = new HashSet<>();
        Stack<String> words = new Stack<>();

        // dAttack[] is our 'string'
        char[] dAttack = {'o', 'i', 'z', 'a', 'a', 'o', 'r', 'h'};

        HashSet<Character> characters = new HashSet<>();
        for (char c : dAttack) {
            if (characters.contains(c)) continue;
            characters.add(c);
        }
        StdOut.println(characters.size());
        maxPermutations = factorial(characters.size())*dAttack.length;
        StdOut.println(maxPermutations);

        // for testing purposes:
        long startTime = System.nanoTime();

        int numPermutations = 0;

        while (numPermutations < maxPermutations) {
            // we'll need to keep track of the "pivot" letter (the 1 letter from the board, plus its location)
            int oi = (dAttack.length - 1);
            int j = (int) (Math.random() * oi);
            char t = dAttack[j];
            dAttack[j] = dAttack[oi];
            dAttack[oi] = t;
            String perm = new String(dAttack);
            if (!tried.contains(perm)) {
                tried.add(perm);
                System.out.print(++numPermutations + ": ");
                for (int k = 0; k < dAttack.length; k++) {
                    System.out.print(dAttack[k]);
                    // for testing purposes
                }
                words.push(String.copyValueOf(dAttack));
                System.out.println();
            }
        }

        // for testing purposes
        long endTime = System.nanoTime();

        System.out.printf("\n\n Permutation Process over. Elapsed Time: %d\n", endTime - startTime);

        In in = new In("words.txt");
        for (String word : in.readAllLines()) {
            dictionary.add(word);
        }

        Out out = new Out("guesses.txt");


        HashSet<String> messages = new HashSet<>();

        startTime = System.nanoTime();
        while (!words.isEmpty()) {
            String word = words.pop();
            out.println();
            out.println(word);
            for (int wordLength = 2; wordLength <= length; wordLength++) {
                String guess = word.substring(0, wordLength);
                if (dictionary.contains(guess)) {
                    if (!messages.contains(guess)) {
                        System.out.println(guess);
                        messages.add(guess);
                    }
                }
            }
        }
        endTime = System.nanoTime();
        System.out.printf("\n\n Dictionary Lookup Process over. Elapsed Time: %d\n", endTime - startTime);
    }
}
