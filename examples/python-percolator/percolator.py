from subprocess import Popen,PIPE

lines = [
    "The quick brown fox jumps over the lazy dog",
    "not matching"
]

queries_file = "queries.json"

p2 = Popen(["lmgrep", "--queries-file=" + queries_file, "--with-empty-lines"], stdin=PIPE, stdout=PIPE, universal_newlines=True)

for line in lines:
    p2.stdin.write(line.strip() + "\n")
    out = p2.stdout.readline().strip()
    if out:
        print("MATCH:>>>" + out + "<<<")
    else:
        print("Line: >>>" + line + "<<< didn't match.")

p2.stdin.close()
print("\nAll lines were processed.")
