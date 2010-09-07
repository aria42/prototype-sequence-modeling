package edu.berkeley.nlp.util.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import edu.berkeley.nlp.tokenizer.PTBLineLexer;
import edu.berkeley.nlp.tokenizer.PTBTokenizer;
import edu.berkeley.nlp.treebank.PennTreebankLanguagePack;
import edu.berkeley.nlp.treebank.TreebankLanguagePack;


public class DocumentSentenceSegmenter {
	
	public List<List<String>> getSentences(File file) {		
		StringBuilder data = new StringBuilder() ;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				data.append(line + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}		
		return getSentences(data.toString());		
	}
	
	public List<List<String>> getSentences(String docText) {
		TreebankLanguagePack langPack = new PennTreebankLanguagePack();
		List<String> puncToks = Arrays.asList(langPack.sentenceFinalPunctuationWords());
		List<List<String>> sents = new ArrayList<List<String>>();
		List<String> curSent = new ArrayList<String>();
		try {
			PTBTokenizer toker = new PTBTokenizer(new StringReader(docText), false);
			List<String> allToks = toker.tokenize();
			for (String tok: allToks) {
				curSent.add(tok);
				if (puncToks.contains(tok)) {				
					sents.add(curSent);
					curSent = new ArrayList<String>();
				}
			}
			if (!curSent.isEmpty()) {
				sents.add(curSent);
//				throw new RuntimeException();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sents;
	}
	
	public DocumentSentenceSegmenter() {
		
	}

		
}
