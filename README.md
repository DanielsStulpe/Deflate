# File Compression and Decompression with Deflate Algorithm
This Java project implements file compression and decompression using the Deflate algorithm, which combines LZ77 and Huffman coding techniques. The program offers a command-line interface for compressing, decompressing, comparing file sizes, and checking if two files are equal.

# Features
- Compression (`comp`): Compresses a given source file and outputs the result to an archive.
- Decompression (`decomp`): Decompresses a given archive and outputs the result to a specified file.
- Size Comparison (`size`): Displays the size of a specified file in bytes.
- File Equality Check (`equal`): Checks if two files are identical.
- About Information (`about`): Displays information about the developer.

# How to Run
To use this project, ensure you have Java installed on your machine. You can run the Main.java file using your preferred Java IDE or the command line. Here are the steps to run the program from the command line:

### 1. Compile the Java program:

```
javac Main.java
```

### 2. Run the compiled Java program:

```
javac Main.java
```

### 3. Follow the prompts in the command-line interface to use the available features.

# Usage Examples
The following examples demonstrate some of the available commands:

- **Compression**:
  - Input: `comp`
  - Prompts:
    - "source file name:" (e.g., *File1.html*)
    - "archive name:" (e.g., *File1.compressed*)
  - Description: Compresses File1.html and stores the compressed data in File1.compressed.
- **Decompression**:
  - Input: `decomp`
  - Prompts:
    - "archive name:" (e.g., *File1.compressed*)
    - "file name:" (e.g., *DecompressedFile1.html*)
  - Description: Decompresses File1.compressed and stores the decompressed content in DecompressedFile1.html.
- **Size Comparison**:
  - Input: `size`
  - Prompts:
    - "file name:" (e.g., *File1.html*)
  - Description: Displays the size of File1.html in bytes.
- **File Equality Check**:
  - Input: `equal`
  - Prompts:
    - "first file name:" (e.g., *File1.html*)
    - "second file name:" (e.g., *DecompressedFile1.html*)
  - Description: Checks if File1.html and DecompressedFile1.html are identical.
- **About Information**:
  - Input: `about`
  - Description: Displays information about the developer.

# Included Files
Along with Main.java, the repository contains an examples folder with the following files:

- Java.html
- JavaScript.html
- RTU.html
- Rainis.html
- MonteCarlo.html
  
These files can be used to test and demonstrate the functionality of the compression and decompression features.

# Results

| File name       | File size (KB) | Comp file size (KB) | Comp ratio | Comp coef. | Comp time (ms) | Decomp time (ms) |
|-----------------|----------------|---------------------|------------|------------|----------------|------------------|
| Java.html       | 80.479         | 33.356              | 58.55      | 2.41       | 663            | 67               |
| JavaScript.html | 344.523        | 107.19              | 68.89      | 3.21       | 2544           | 64               |
| RTU.html        | 83.069         | 30.93               | 62.77      | 2.69       | 518            | 14               |
| Rainis.html     | 206.694        | 57.458              | 72.20      | 3.60       | 1321           | 17               |
| MonteCarlo.html | 612.88         | 100.359             | 83.63      | 6.11       | 2331           | 23               |


# Credits
This project was developed by Daniels Stulpe, a student at Riga Technical University.
