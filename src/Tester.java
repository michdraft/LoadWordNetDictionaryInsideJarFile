import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

/**
 * This package allows to load a WordNet dictionary which is located inside a
 * JAR file. <br/>
 * It implements the IDataProvider, IDataSource & Dictionary classes which can
 * be used with JWI (the MIT Java Wordnet Interface).
 * 
 * @author Markus HAENSE
 */
public class Tester {

	public static void main(String[] args) {
		// TODO: can be improved ;)
		// Path to the WordNet directory inside the JAR file
		JarFileProvider.WORDNET_PATH = "WordNet-3.0/dict/";

		// Construct the dictionary object and open it
		IDictionary wordnetDictionary = new JarDictionary(
				Tester.class.getResource(""));
		wordnetDictionary.open();

		// Look up first sense of the word "dog"
		IIndexWord idxWord = wordnetDictionary.getIndexWord("dog", POS.NOUN);
		IWordID wordID = idxWord.getWordIDs().get(0);
		IWord word = wordnetDictionary.getWord(wordID);

		System.out.println("Id = " + wordID);
		System.out.println("Lemma = " + word.getLemma());
		System.out.println("Gloss = " + word.getSynset().getGloss());

		wordnetDictionary.close();
	}
}