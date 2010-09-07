#!/usr/bin/ruby

SCRIPT = File.basename(__FILE__)
BASE = File.dirname(File.dirname(File.expand_path(__FILE__)))
CLASS = "edu.berkeley.nlp.prototype.pos.PartOfSpeechTester"
def classpathString
  jars = Dir["#{BASE}/lib/*jar"]
  return jars.join(":")
end

def usage
  puts "[usage] %s conf-file\n" % SCRIPT
  puts "\tstartSection\t\t\tWhich section to start with in Treebank [2]"
  puts "\tstopSection\t\t\tWhich section to stop in Treebank [24]"
  puts "\tmaxNumSentences\t\t\tHow many sentences to train on [2000]"
  puts "\ttreebankPath\t\t\tPath to wsj/ directory of Treebank [required]"  
  puts "\tprotoThresh\t\t\tWhat fraction of a word usage do we need to have to be a prototype [0.5]"
  puts "\tnumProtosPerTag\t\t\tHow many prototypes per tag [3]"
  puts "\tsimModelPath\t\t\tPath to similarity model []"
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
classpathString = classpathString()
confFile = ARGV[0]
cmd = "java -server -mx1200m -cp #{classpathString} #{CLASS} ++#{confFile}"
exec(cmd)
