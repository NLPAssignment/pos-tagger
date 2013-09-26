`pos-tagger` is an assignment for the CS626 (Natural Language Processing) course at IIT-Bombay. It deals with the problem of Part-of-Speech tagging.

The corpus used is a simplified POS-tagged corpus consisting of nearly 11,000 words with the following tagset:

* N - Noun
* V - Verb
* A - Adjective
* R - Adverb
* O - Everything else including punctuation

`pos-tagger` uses the HMM-based [Viterbi Algorithm](http://en.wikipedia.org/wiki/Viterbi_algorithm) to tag a given sentence.