require 'open3'
require 'timeout'
require 'json'

class Percolator

  def initialize(dictionary_file_path, lmgrep_path='lmgrep', params='', timeout=1)
    @timeout = timeout
    command = "#{lmgrep_path} --queries-file=#{dictionary_file_path} #{params} --format=json --with-empty-lines --with-details --with-scored-highlights"
    @stdin, @stdout, @stderr, @wait_thr = Open3.popen3(command)

    # prevent leaking file descriptors
    ObjectSpace.define_finalizer(self, Proc.new do
      close
      puts ">>>Percolator is closed.<<<"
    end)
  end

  def close
    @stdin.close
    @stdout.close
    @stderr.close
  end

  # Returns true if matches, false otherwise
  def matches?(text)
    @stdin.puts text
    # lmgrep works like this:
    # - if an input doesn't match any query then there is an empty line writen to stdout
    # - if there is a match then some JSON output is returned
    # The percolation is timeout bound.
    # If it times-out, stop the percolator and
    # - throw an exception
    # - log the output
    # - create an issue here https://github.com/dainiusjocas/lucene-grep/issues
    output = Timeout::timeout(@timeout) {
      @stdout.gets
    }
    # If we got any non-blank output then the text matches some query
    ! output.strip.empty?
  rescue Timeout::Error
    puts "Percolation failed on '#{text}'"
    raise "Percolation timed-out!!!"
  end

  # returns the actual output of lmgrep
  def match(text)
    @stdin.puts text
    output = Timeout::timeout(@timeout) {
      @stdout.gets
    }
    return nil if output.strip.empty?
    JSON.parse(output)
  rescue Timeout::Error
    puts "Percolation failed on '#{text}'"
    raise "Percolation timed-out!!!"
  end
end

def percolate_with_bench(percolator, text)
  start = Time.now
  matched = percolator.matches? text
  finish = Time.now
  puts "Percolator on text '#{text}' matches: #{matched}, in: #{finish - start}s"
end

def percolate_for_match_with_bench(percolator, text)
  start = Time.now
  matched = percolator.match text
  matched = matched if matched
  finish = Time.now
  puts "Percolator on text '#{text}' matched: '#{matched}' in: #{finish - start}s"
end

dictionary_file_path = 'queries.json'
percolator = Percolator.new(dictionary_file_path)

puts "Given the Percolator dictionary: #{JSON.parse(File.read(dictionary_file_path))}"

matching_text = "The quick brown fox jumps over the lazy dog"
non_matching_text = "not matching"

puts
puts "Checks if the text matches:"

percolate_with_bench(percolator, matching_text)
percolate_with_bench(percolator, non_matching_text)

puts
puts ">>>The matches in are returned<<<"

percolate_for_match_with_bench(percolator, matching_text)
percolate_for_match_with_bench(percolator, non_matching_text)
