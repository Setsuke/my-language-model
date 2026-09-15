# MyLanguageModel

Java Swing application that analyzes a text file with a custom hash table and builds a small n-gram language model.

This project was developed as coursework for the MSc Advanced Computer Science module **Object Oriented Programming and Software Design**.

## What it does

- Loads a `.txt` document from the desktop interface.
- Tokenizes and counts words.
- Stores word frequencies in a custom hash table with linked-list collision handling.
- Compares three hash functions:
  - First-letter hash
  - Polynomial rolling hash
  - DJB2 hash
- Shows hash table statistics such as standard deviation, minimum bucket size, maximum bucket size, and non-empty buckets.
- Builds unigram, bigram, and trigram tables.
- Generates simple text from a user-provided seed using bigram or trigram frequency counts.

## Why this project is useful

This project demonstrates core software development concepts:

- Object-oriented programming in Java
- Custom data structures
- Hash functions and collision handling
- File processing
- Basic natural language processing with n-grams
- Java Swing desktop UI

## Project structure

```text
src/
  MyLanguageModel.java          Main Swing application and language model logic
  MyHashTable.java              Custom hash table
  MyLinkedObject.java           Linked-list node used for collisions
  MyHashFunction.java           Base class for hash functions
  FirstLetterHash.java          Simple first-character hash
  PolynomialRollingHash.java    Polynomial rolling hash
  DJB2Hash.java                 DJB2 hash implementation
  news.txt                      Sample text corpus
```

## How to run

From the project root:

```bash
javac -d out src/*.java
java -cp out MyLanguageModel
```

Or open the project in IntelliJ IDEA and run `MyLanguageModel.main()`.

## Current limitations

- No automated tests yet.
- The project does not currently use Maven or Gradle.
- The UI, tokenization, model building, and text generation logic are mostly in one class.
- Text generation is deterministic and always chooses the most frequent next word.
- The tokenizer is simple and focused on lowercase English text.

## Planned improvements

- Add unit tests for the hash table, tokenizer, and n-gram generation.
- Move the project to a Maven or Gradle structure.
- Separate the UI from the core language model logic.
- Add a cleaner demo mode with the included sample corpus.
- Improve text generation with weighted random sampling.
- Add screenshots and example outputs.
