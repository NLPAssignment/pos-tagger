import sys

fr = open(sys.argv[1], "r")
fw = open(sys.argv[1] + "-rep", "w")

for line in fr:
    newline = line.replace("TO0", "PRP")
    fw.write(newline)
