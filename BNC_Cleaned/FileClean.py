import re

fread = open("FullCorpus.txt")

# Read the entire file
str1 = fread.read()

# Perform the replacements
str2 = re.sub("(PUN_\. |PUN_\? |PUN_\! )", "\\1\n", str1)

fwrite = open("FullCorupus-Cleaned.txt", "w")

fwrite.write(str2)

fread.close()
fwrite.close()
