#!/usr/bin/ruby

SCRIPT = File.basename(__FILE__)
BASE = File.dirname(File.dirname(File.expand_path(__FILE__)))
CLASS = "edu.berkeley.nlp.prototype.PrototypeSequenceModelTrainer"
def classpathString
  jars = Dir["#{BASE}/lib/*jar"]
  return jars.join(":")
end
def usage
  puts "[usage] %s conf-file\n" % SCRIPT
  puts "\n\tData Processing Options\n\t----------------------------"
  puts "\tdataRoot\t\tDirectory containing data-files, each file one sentence per-line [required]"  
  puts "\tprefix\t\t\tPrefix of data files to consider []"
  puts "\textension\t\tExtension of files to consider. If you pass .gz as a suffix, we will unzip data"
  puts "\tmaxNumSentences\t\tMaximum number of sentences to use [2147483647]"
  puts "\n\tSequence Model Options \n\t-----------------------"
  puts "\torder\t\t\tMarkov order of sequence model [2]"
  puts "\tnumIters\t\tNumber of iterations [100]"
  puts "\tminIters\t\tMinimum number of iterations [50]"
  puts "\tsigmaSquared\t\tL2 Penalty Parameter [0.0]"
  puts "\tverbose\t\t\tOnly if you want to see lots of data go by [false]"  
  puts "\tnumCPUS\t\t\tHow many CPUs to use. Defaults to number Available"
  puts "\tprotoFile\t\tPrototype File [required]"
  puts "\toutfile\t\t\tSequence Model Outfile []"
  puts "\n\tWord Feature Extraction Options \n\t-------------------------------"
  puts "\tuseSuffixFeatures\t[false]"
  puts "\tsuffixLength\t\tHow long suffixes to use as features. [3]"
  puts "\tuseInitialCapital\t[false]"
  puts "\tuseHasDigit\t\t[false]"
  puts "\tuseHasHyphen\t\t[false]"
  puts "\tnumSimilarWords\t\tMaximum Number of prototype similarity features per word [3]"
  puts "\tsimThreshold\t\tThreshold for similarity to consider a similarity feature [0.35]"
  puts "\n"
end

if ARGV.length == 0
  usage()
  exit(2)
end
confFile = ARGV[0]
classpathString = classpathString()
cmd = "java -server -mx1200m -cp #{classpathString} #{CLASS} ++#{confFile}"
exec(cmd)
