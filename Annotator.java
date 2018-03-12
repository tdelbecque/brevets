import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.Properties;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;

/*
 This annotates a set of patents.
 
 To compile it:
 javac -cp "*" -Xlint Annotator.java

 To run it, given you are in the Stanford core NLP directory:
 java -cp ".:*" Annotator /var/www/brat/data/brevet

 The first argument is the directory where the patent abstracts are. 
 Each patent abstract must be in a .txt file. The annotation is written 
 in the same directory, but the file extension is replaced by .ann. 
 For exemple foo.txt generates foo.ann. In this way the directory can
 be understood by Brat (note that /var/www/brat/ is the root directory 
 of Brat on Pollux)

*/
public class Annotator {
    static StanfordCoreNLP pipeline;
    static String directoryPathString;

    // map the tagger PoS to  ?
    static Map<String,String> postagToCategoryMap = new TreeMap <>();
    static {
        postagToCategoryMap.put ("DT", "dt");
        postagToCategoryMap.put ("NN", "noun");
        postagToCategoryMap.put ("JJ", "adj");
        postagToCategoryMap.put ("RB", "adv");
    }

    static boolean tokenIsMeaningful (CoreLabel token) {
        String tag = token.tag ();
        if (tag.compareTo (".") == 0) return false;
        if (tag.compareTo (",") == 0) return false;
        if (tag.compareTo ("DT") == 0) return false;
        if (tag.compareTo ("IN") == 0) return false;
        if (tag.compareTo ("CC") == 0) return false;
        if (tag.compareTo ("TO") == 0) return false;
        return true;
    }
    
    static String categoryFromPostag (String postag) {
        return postagToCategoryMap.getOrDefault (postag, postag);
    }

    /*
      Output an annotation in Brat format: loop in sentences (1) and tokens
      in sentences (2)
      Create entities, with an id that begin with "T" and the 'entityCounter'
      running counter (3). When all the tokens in a sentence have been processed, 
      the relations are output (4).

      The relations are available through SemanticGraph objects. 
      Nodes of these graphs (dependent and governor) are object of class IndexWord. 
      To get the Brat id of such an IndexWord we use the 'indexedWordToBratIdMap' (5)
      that is populated during the previous loop across the tokens (6).
    */
    static void outputAnnotation (Annotation annotation, Path outPath)
        throws IOException
    {
        try (BufferedWriter writer = Files.newBufferedWriter (outPath)) {
            Map<IndexedWord, String> indexedWordToBratIdMap = new HashMap<>(); // (5)
            int entityCounter = 1;
            int relationCounter = 1;
            List<CoreMap> sentences = annotation.get (CoreAnnotations.
                                                      SentencesAnnotation.class);
            for (CoreMap sentence : sentences) { // (1)
                for (CoreLabel token : sentence.get (CoreAnnotations.
                                                     TokensAnnotation.class)) { // (2)
                    if (tokenIsMeaningful (token)) {
                        int beginPosition = token.beginPosition ();
                        int endPosition = token.endPosition ();
                        String category = categoryFromPostag (token.tag ());
                        String bratId = "T" + (entityCounter ++); // (3)
                        indexedWordToBratIdMap.put (new IndexedWord (token), bratId); // (6)
                        writer.write (bratId + "\t" + 
                                      category + " " +
                                      beginPosition + " " + endPosition + "\t" +
                                      token.originalText () + "\n");
                    }
                }
                SemanticGraph g = sentence.get (SemanticGraphCoreAnnotations.
                                                EnhancedPlusPlusDependenciesAnnotation.class);
                for (SemanticGraphEdge e : g.edgeIterable ()) { // (4)
                    IndexedWord dep = e.getDependent ();
                    IndexedWord gov = e.getGovernor ();
                    GrammaticalRelation rel = e.getRelation ();
                    String depId = indexedWordToBratIdMap.get (dep);
                    String govId = indexedWordToBratIdMap.get (gov);
                    if ((depId != null) && (govId != null)) {
                        String bratId = "R" + (relationCounter ++);
                        String relLabel = rel.getSpecific () == null ?
                            rel.getShortName () :
                            rel.getSpecific ();
                        writer.write (bratId + "\t" +
                                      relLabel + " gov:" + govId + " dep:" + depId + "\n");
                    }
                }
            }
        }
    }

    /*
      Compute the annotation and output the result. 
      'p' is the path to the file to process. Only regular files
      which name ends with ".txt" are processed.
    */
    static void compute (Path p) {
        if (Files.isRegularFile (p)) {
            String fileName = p.getFileName ().toString ();
            if (fileName.endsWith (".txt")) {
                System.out.println (fileName);
                try {
                    byte [] bs = Files.readAllBytes (p);
                    String text = new String (bs, "UTF-8");
                    Annotation annotation = new Annotation (text);
                    pipeline.annotate (annotation);
                    Path outPath = FileSystems.getDefault ().
                        getPath (directoryPathString,
                                 fileName.substring(0, fileName.lastIndexOf("."))+ ".ann");
                    outputAnnotation (annotation, outPath);
                }
                catch (Exception e) {
                    System.err.println ("Error for " + fileName);
                }
            }
        }
    }

    /*
      Set up a pipeline (1), then browse through the directory given as argument 
      for all files.
    */
    public static void main (String[] args) throws Exception {
        Properties props = new Properties ();
        props.setProperty ("annotators", 
                           "tokenize,ssplit,pos,lemma,depparse");
        pipeline = new StanfordCoreNLP (props); // (1)
        
        directoryPathString = args [0];
        Path directoryPath = FileSystems.getDefault ()
            .getPath (directoryPathString);
        DirectoryStream<Path> stream = Files.newDirectoryStream (directoryPath);
        for (Path p: stream)
            compute (p);
    }
}
