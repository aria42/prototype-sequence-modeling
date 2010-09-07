#!/usr/bin/ruby

SCRIPT = File.basename(__FILE__)
BASE = File.dirname(File.dirname(File.expand_path(__FILE__)))
CLASS = "edu.berkeley.nlp.prototype.PrototypeSequenceModelTester"

def classpathString
  jars = Dir["#{BASE}/lib/*jar"]
  return jars.join(":")
end

def options(confFile)
  open(confFile) do |f|
    opts = Hash.new
    f.readlines().each do |line|
      opt, val = line.split(/\s+/)
      opts[opt] = val
    end
    return opts
  end  
end

def usage
  puts "[usage] %s conf-file\n" % SCRIPT
  puts "\tinDirRoot\tDirectory containing data-files to tag, each file one sentence per-line [required]"
  puts "\tinExtension\tExtension of data files []"
  puts "\tinPrefix\tPrefix of data files to consider []"
  puts "\toutDir\tDirectory to put tagged data [required]"
  puts "\toutExtension\tExtension to add to tagged files [tagged]"
  puts "\tdelimeter\tDelimeter to use between output word and tag [\#\#]"
  puts "\tmodelPath\tPath to sequence model file"
  puts "\n"
end

if ARGV.length == 0
  usage()
  exit(2)
end
classpathString = classpathString()
confFile = ARGV[0]
opts = options(confFile)
outDir = opts['outDir']
if !File.exists?(outDir)
  puts "Creating #{outDir}"
  system("mkdir -p #{outDir}")
end
cmd = "java -ea -server -mx1200m -cp #{classpathString} #{CLASS} ++#{confFile}"
exec(cmd)
