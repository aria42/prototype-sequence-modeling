#!/usr/bin/ruby

SCRIPT = File.basename(__FILE__)
BASE = File.dirname(File.dirname(File.expand_path(__FILE__)))
CLASS = "edu.berkeley.nlp.prototype.simmodel.WordContextSimilarity"

def classpathString
  jars = Dir["#{BASE}/lib/*jar"]
  return jars.join(":")
end

def usage
  puts "[usage] %s conf-file\n" % SCRIPT
  puts "\n\tData Processing Options\n\t-----------------------"
  puts "\tdataRoot\t\t\tDirectory containing data-files, each file one sentence per-line [required]"  
  puts "\tprefix\t\t\t\tPrefix of data files to consider []"
  puts "\textension\t\t\tExtension of files to consider. If you pass .gz as a suffix, we will unzip data"
  puts "\tmaxNumSentences\t\t\tMaximum number of sentences to use [2147483647]"
  puts "\n\tContext Model Options\n\t-----------------------"
  puts "\tdirectional\t\t\tShould we care about direction for context feature?  [false]"
  puts "\tcontextWindow\t\t\tHow large is context window size? [2]"
  puts "\tnumContextWords\t\t\tHow many most frequent words to use for context features [1500]"
  puts "\treducedDimension\t\tIf you reduce data how many dimensions [50]"
  puts "\treduceType\t\t\tWhat (if any) type of reduction to use: SVD (Must have Matlab installed and in path)," +
       "\n\t\t\t\t\tRAND_PROJ (Random Projection), or NONE"
  puts "\toutfile\t\t\t\tWhere to write model []"
  puts "\n"
end

if ARGV.length == 0
  usage()
  exit(2)
end

confFile = ARGV[0].strip
classPathString = classpathString()
cmd = "java -server -mx1200m -cp #{classpathString} #{CLASS} ++#{confFile}" 
exec(cmd)


