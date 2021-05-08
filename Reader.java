import java.util.Scanner;
import java.util.ArrayList;

import java.io.FileInputStream; 
import java.io.InputStream; 
import java.io.FileNotFoundException; 
import java.io.IOException;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

import opennlp.tools.sentdetect.SentenceDetectorME; 
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME; 
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.postag.POSModel; 
import opennlp.tools.postag.POSSample; 
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.cmdline.parser.ParserTool; 
import opennlp.tools.parser.Parse; 
import opennlp.tools.parser.Parser; 
import opennlp.tools.parser.ParserFactory; 
import opennlp.tools.parser.ParserModel;
import opennlp.tools.chunker.ChunkerME; 
import opennlp.tools.chunker.ChunkerModel; 
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.util.Span;
import opennlp.tools.util.InputStreamFactory;
//import java.io.ObjectStreamClass;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.ModelUtil;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.lemmatizer.LemmatizerME;
import opennlp.tools.lemmatizer.LemmatizerModel;

public class Reader
{
    public static void main(String args[]) throws Exception {
        Reader r = new Reader();
        ArrayList<String> wordsInitial = new ArrayList<String>();
        try {
            File textFile = new File("../ocr_server/ingredients.txt");
            Scanner sc = new Scanner(textFile);
            while (sc.hasNextLine())
            {
                wordsInitial.add(sc.nextLine());
            }
            sc.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        System.out.print("Please enter the delimiter: ");
        Scanner sc = new Scanner(System.in);
        String delimiter = sc.nextLine();
        sc.close();
        ArrayList<String> words = r.processInput(wordsInitial, delimiter);
        ArrayList<String> wordsCorrected = new ArrayList<String>();
        for (String w : words)
        {
            try {wordsCorrected.add(r.findCategory(w));}
            catch (Exception e) {System.out.println("Exception thrown");}
        }
        for (String w : wordsCorrected)
        {
            System.out.println(w);
        }
    }

    private ArrayList<String> processInput(ArrayList<String> text, String d)
    {
        ArrayList<String> words = new ArrayList<>(); 
        for (String s : text)
        {
            while (s.contains(d))
            {
                words.add(s.substring(0, s.indexOf(d)));
                s = s.substring(s.indexOf(d) + 1);
            }
            words.add(s);
        }
        return words;
    }

    private String findCategory(String s) throws Exception { 
        // String[] sents = detectSentences(s);
        String[] tokens = tokenize(s);
        String[] posTags = posTag(tokens);
        String[] lemmas = lemmatize(tokens, posTags);
        DoccatModel model = trainCategorizerModel();
        return categorize(model, lemmas);
    }

    /*public static String[] detectSentences(String s) throws Exception
    {   
        InputStream input = new FileInputStream("/model/en-sent.bin");
        SentenceModel model = new SentenceModel(input);
        SentenceDetectorME detector = new SentenceDetectorME(model);
        String sentences[] = detector.sentDetect(s);
        return sentences;
    }*/
    
    public static String[] tokenize(String s) throws Exception
    {
        InputStream input = new FileInputStream("../nlp/model/en-token.bin");
        TokenizerModel model = new TokenizerModel(input);
        TokenizerME tokenizer = new TokenizerME(model);
        String[] tokens = tokenizer.tokenize(s);
        return tokens;
    }

    public static String[] posTag(String[] tokens) throws Exception
    {       
        InputStream input = new FileInputStream("../nlp/model/en-pos-maxent.bin"); 
        POSModel model = new POSModel(input); 
        POSTaggerME tagger = new POSTaggerME(model); 
        String[] tags = tagger.tag(tokens);
        return tags;
    }

    public static String[] lemmatize(String[] tokens, String[] posTags) throws Exception
    {
        InputStream input = new FileInputStream("../nlp/model/en-lemmatizer.bin");
        LemmatizerModel model = new LemmatizerModel(input);
        LemmatizerME categorizer = new LemmatizerME(model);
        String[] lemmaTokens = categorizer.lemmatize(tokens, posTags);    
        return lemmaTokens;
    }

    public static DoccatModel trainCategorizerModel() throws FileNotFoundException, IOException {
        InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(new File("chemicals.txt"));
        ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
        ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);
        DoccatFactory factory = new DoccatFactory(new FeatureGenerator[] { new BagOfWordsFeatureGenerator() }); 
        TrainingParameters params = ModelUtil.createDefaultTrainingParameters();
        params.put(TrainingParameters.CUTOFF_PARAM, 0);
        DoccatModel model = DocumentCategorizerME.train("en", sampleStream, params, factory);
        return model;
    }

    public static String categorize(DoccatModel model, String[] tokens) throws IOException
    {
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
        double[] probabilitiesOfOutcomes = myCategorizer.categorize(tokens);
        String category = myCategorizer.getBestCategory(probabilitiesOfOutcomes);
        System.out.println("Category: " + category);
        return category; 
    }
}