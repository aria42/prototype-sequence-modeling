package edu.berkeley.nlp.util;

import java.io.File;
import java.io.FileFilter;

public class SuffixFilter implements FileFilter {

	private String suffix;

	public SuffixFilter(String suffix) {
		this.suffix = suffix;
	}

	public boolean accept(File f) {
		if (f.isDirectory()) return true;
		String name = f.getName();
		return name.endsWith(suffix);
	}

}
