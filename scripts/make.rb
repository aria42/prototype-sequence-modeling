#!/usr/bin/ruby
BASE = File.dirname(File.dirname(File.expand_path(__FILE__)))
system("cd #{BASE} && ant jar && ant clean")