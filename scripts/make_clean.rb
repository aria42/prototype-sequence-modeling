#!/usr/bin/ruby
BASE = File.dirname(File.dirname(File.expand_path(__FILE__)))
system("rm -f models/*")
system("rm -f output/*")
system("cd #{BASE} && ant clean")