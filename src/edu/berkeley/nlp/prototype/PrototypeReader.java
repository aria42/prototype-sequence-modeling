package edu.berkeley.nlp.prototype;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fig.basic.IOUtils;

public class PrototypeReader {
	
	public static Map<String, Set<String>> readProtoypeMap(String path) {
		Map<String, Set<String>> protoMap = new HashMap<String, Set<String>>();
		try {
			for (String line: IOUtils.readLines(path)) {
//				if (line.length() == 0 || line.startsWith("#")) {
//					continue;
//				}
				String[] fields = line.split("\t");
				String label = fields[0];
				Set<String> words = new HashSet<String>();
				for (int i=1; i < fields.length; ++i) {
					words.add(fields[i]);
				}
				protoMap.put(label, words);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error Reading Prototype file " + path);
		}	
		return protoMap;		
	}
	
	public static void main(String[] args) {
		String protoFile = args[0];
		Map<String, Set<String>> protoMap = readProtoypeMap(protoFile);
		System.out.println(protoMap);
	}
	
}
