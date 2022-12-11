package scrabble;

public class ScrabbleWinnerGym {
    public static void main(String[] args) {
        ScrabbleWinner.DictGraph dictGraph = new ScrabbleWinner.DictGraph("words.txt");
        dictGraph.printTest(5000);
    }
}
