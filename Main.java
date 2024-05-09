import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Comparator;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String choice;
        String sourceFile, resultFile;

        while (true) {
            System.out.println("Enter command (comp, decomp, size, equal, about, exit):");
            choice = sc.next().trim();

            switch (choice) {
                case "comp":
                    System.out.print("source file name: ");
                    sourceFile = sc.next().trim();
                    System.out.print("archive name: ");
                    resultFile = sc.next().trim();
                    compress(sourceFile, resultFile);
                    break;

                case "decomp":
                    System.out.print("archive name: ");
                    sourceFile = sc.next().trim();
                    System.out.print("file name: ");
                    resultFile = sc.next().trim();
                    decompress(sourceFile, resultFile);
                    break;

                case "size":
                    System.out.print("file name: ");
                    sourceFile = sc.next().trim();
                    size(sourceFile);
                    break;

                case "equal":
                    System.out.print("first file name: ");
                    String firstFile = sc.next().trim();
                    System.out.print("second file name: ");
                    String secondFile = sc.next().trim();
                    System.out.println(equal(firstFile, secondFile));
                    break;

                case "about":
                    about();
                    break;

                case "exit":
                    sc.close();
                    return;

                default:
                    System.out.println("Unknown command. Try again.");
                    break;
            }
        }
    }

    public static void compress(String sourceFile, String resultFile) {
        try {
            // Read text from the source file
            StringBuilder inputText = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(sourceFile), Charset.forName("UTF-8")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    inputText.append(line).append("\n");
                }
            }

            // Compress the text using LZ77
            List<Tag> lz77Tags = LZ77.encode(inputText.toString());
            BitSet lz77BitSet = LZ77.convertTagsToBits(lz77Tags);
            byte[] lz77Bytes = lz77BitSet.toByteArray();

            // Further compress the LZ77 output using Huffman coding
            Huffman huffman = new Huffman();
            byte[] huffmanCompressed = huffman.compress(lz77Bytes);

            // Write the compressed data to the output file
            writeBinaryFile(huffmanCompressed, resultFile);

            System.out.println("Compression complete.");
        } catch (IOException e) {
            System.err.println("Error during compression: " + e.getMessage());
        }
    }

    public static void decompress(String sourceFile, String resultFile) {
        try {
            // Read the compressed data from the file
            byte[] compressedData = readBinaryFile(sourceFile);

            // Decompress the data using Huffman coding
            Huffman huffman = new Huffman();
            byte[] huffmanDecompressed = huffman.decompress(compressedData);

            // Decompress the Huffman output using LZ77
            BitSet lz77BitSet = BitSet.valueOf(huffmanDecompressed);
            List<Tag> lz77Tags = LZ77.convertBitsToTags(lz77BitSet);
            String decompressedText = LZ77.decompressFromTags(lz77Tags);

            // Write the decompressed text to the output file
            writeDecompressedToFile(decompressedText, resultFile);

            System.out.println("Decompression complete.");
        } catch (IOException e) {
            System.err.println("Error during decompression: " + e.getMessage());
        }
    }

    public static void size(String sourceFile) {
        try {
            FileInputStream f = new FileInputStream(sourceFile);
            System.out.println("size: " + f.available() + " bytes");
            f.close();
        } catch (IOException ex) {
            System.out.println("Error getting file size: " + ex.getMessage());
        }
    }

    public static boolean equal(String firstFile, String secondFile) {
        try {
            FileInputStream f1 = new FileInputStream(firstFile);
            FileInputStream f2 = new FileInputStream(secondFile);
            int k1, k2;
            byte[] buf1 = new byte[1000];
            byte[] buf2 = new byte[1000];
            do {
                k1 = f1.read(buf1);
                k2 = f2.read(buf2);
                if (k1 != k2) {
                    f1.close();
                    f2.close();
                    return false;
                }
                for (int i = 0; i < k1; i++) {
                    if (buf1[i] != buf2[i]) {
                        f1.close();
                        f2.close(); 
                        return false;
                    }
                }
            } while (!(k1 == -1 && k2 == -1));

            f1.close();
            f2.close();
            return true;
        } catch (IOException ex) {
            System.out.println("Error comparing files: " + ex.getMessage());
            return false;
        }
    }

    public static void about() {
        System.out.println("Developed by RTU student Daniels Stulpe");
    }

    private static byte[] readBinaryFile(String filename) throws IOException {
        return Files.readAllBytes(Paths.get(filename));
    }

    private static void writeBinaryFile(byte[] data, String filename) throws IOException {
        Files.write(Paths.get(filename), data);
    }

    private static void writeDecompressedToFile(String decompressedText, String resultFile) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(resultFile), Charset.forName("UTF-8")))) {
            writer.write(decompressedText);
        } catch (IOException e) {
            System.err.println("Error writing decompressed text to file: " + e.getMessage());
        }
    }
}

// Tag class
class Tag {
    private final short offset;
    private final short length;
    private final char next;
    private final boolean isExtended;

    // Constants for bit manipulation
    public static final int OFFSET_LENGTH_BITS = 16; // Offset and length have 16-bit size
    public static final int CHAR_FLAG_BITS = 1; // Bit flag for character type
    public static final int CHAR_8_BIT = 8; // 8 bits for simple characters
    public static final int CHAR_24_BIT = 24; // 24 bits for extended characters

    // Constructor for creating a Tag object with given attributes
    Tag(short offset, short length, char next, boolean isExtended) {
        this.offset = offset;
        this.length = length;
        this.next = next;
        this.isExtended = isExtended;
    }

    // Getter for the offset attribute
    public short getOffset() {
        return offset;
    }

    // Getter for the length attribute
    public short getLength() {
        return length;
    }

    // Getter for the next character
    public char getNext() {
        return next;
    }

    // Indicates whether the tag uses extended characters
    public boolean isExtended() {
        return isExtended;
    }

    // Converts the tag into a BitSet for serialization
    public BitSet convertToBits() {
        int charBitsLength = isExtended ? CHAR_24_BIT : CHAR_8_BIT; // Determine bit length for the character
        int totalBitsLength = OFFSET_LENGTH_BITS * 2 + CHAR_FLAG_BITS + charBitsLength; // Total length for the BitSet

        BitSet bitSet = new BitSet(totalBitsLength);
        int index = 0;

        // Encode the offset into bits
        for (int i = 0; i < OFFSET_LENGTH_BITS; i++, index++) {
            bitSet.set(index, ((offset >> i) & 1) != 0); // Check if the i-th bit is set
        }

        // Encode the length into bits
        for (int i = 0; i < OFFSET_LENGTH_BITS; i++, index++) {
            bitSet.set(index, ((length >> i) & 1) != 0);
        }

        // Flag to indicate if the character is extended
        bitSet.set(index++, isExtended);

        // Encode the character
        if (isExtended) {
            byte[] charBytes = Character.toString(next).getBytes(Charset.forName("UTF-8")); // Get the UTF-8 bytes
            for (byte b : charBytes) {
                for (int i = 0; i < 8; i++, index++) {
                    bitSet.set(index, ((b >> i) & 1) != 0);
                }
            }
        } else {
            for (int i = 0; i < CHAR_8_BIT; i++, index++) {
                bitSet.set(index, ((next >> i) & 1) != 0); // Encode the character into bits
            }
        }

        return bitSet;
    }

    // Create a Tag from a given BitSet
    public static Tag fromBits(BitSet bitSet) {
        int offset = 0;
        int length = 0;
        boolean isExtended = bitSet.get(OFFSET_LENGTH_BITS * 2); // Check if extended flag is set

        // Read the offset from bits
        for (int i = 0; i < OFFSET_LENGTH_BITS; i++) {
            if (bitSet.get(i)) {
                offset |= (1 << i); // Set the corresponding bit in the offset
            }
        }

        // Read the length from bits
        for (int i = OFFSET_LENGTH_BITS; i < OFFSET_LENGTH_BITS * 2; i++) {
            if (bitSet.get(i)) {
                length |= (1 << (i - OFFSET_LENGTH_BITS));
            }
        }

        int charStart = OFFSET_LENGTH_BITS * 2 + CHAR_FLAG_BITS; // Position to read the character
        char next = '\0'; // Default character

        if (isExtended) {
            byte[] charBytes = new byte[3]; // Three-byte UTF-8 representation
            for (int i = 0; i < 3; i++) {
                byte charByte = 0;
                for (int j = 0; j < 8; j++) {
                    if (bitSet.get(charStart + (i * 8) + j)) {
                        charByte |= (1 << j);
                    }
                }
                charBytes[i] = charByte; // Store the byte
            }
            next = new String(charBytes, Charset.forName("UTF-8")).charAt(0); // Convert to character
        } else {
            byte simpleChar = 0;
            for (int i = 0; i < 8; i++) {
                if (bitSet.get(charStart + i)) {
                    simpleChar |= (1 << i); // Set the corresponding bit
                }
            }
            next = (char) simpleChar;
        }

        return new Tag((short) offset, (short) length, next, isExtended); // Return the new Tag
    }
}

// LZ77 class
class LZ77 {
    private static final int WINDOW_SIZE = 32767; // Maximum size for the sliding window
    private static final int MAX_LENGTH = 1000; // Maximum length for the match

    // Encodes the input text using LZ77
    public static List<Tag> encode(String text) {
        List<Tag> result = new ArrayList<>();
        int pos = 0;

        // Loop through the text to find matches
        while (pos < text.length()) {
            Tag bestMatch = findMatching(text, pos, MAX_LENGTH); // Find the best matching substring
            result.add(bestMatch); // Add the tag to the result
            pos += bestMatch.getLength() + 1; // Move the position forward
        }

        return result;
    }

    // Finds the best matching substring in the sliding window
    private static Tag findMatching(String text, int pos, int maxLength) {
        short offset = 0;
        short length = 0;
        char next = (pos < text.length()) ? text.charAt(pos) : '\0'; // Next character

        int windowStart = Math.max(0, pos - WINDOW_SIZE); // Start of the sliding window

        for (int i = pos - 1; i >= windowStart; i--) {
            int matchLength = 0;

            // Find the longest match
            while (matchLength < maxLength && (pos + matchLength) < text.length() &&
                    text.charAt(i + matchLength) == text.charAt(pos + matchLength)) {
                matchLength++;
            }

            if (matchLength > length) { // If this match is longer, update the offset and length
                offset = (short) (pos - i);
                length = (short) matchLength;
                next = (pos + length < text.length()) ? text.charAt(pos + length) : '\0';
            }
        }

        boolean isExtended = (Character.toString(next).getBytes(Charset.forName("UTF-8")).length > 1); // Check if extended

        return new Tag(offset, length, next, isExtended); // Create the tag with the found data
    }

    // Converts the list of tags into a BitSet for serialization
    public static BitSet convertTagsToBits(List<Tag> tags) {
        BitSet bitSet = new BitSet();
        int currentIndex = 0;

        for (Tag tag : tags) {
            int charBitsLength = tag.isExtended() ? Tag.CHAR_24_BIT : Tag.CHAR_8_BIT; // Determine char bit length
            int totalBitsLength = Tag.OFFSET_LENGTH_BITS * 2 + Tag.CHAR_FLAG_BITS + charBitsLength; // Total bit length

            BitSet tagBits = tag.convertToBits(); // Convert the tag to bits
            for (int i = 0; i < totalBitsLength; i++) {
                bitSet.set(currentIndex + i, tagBits.get(i)); // Set the corresponding bits in the BitSet
            }

            currentIndex += totalBitsLength; // Update the current index
        }

        return bitSet; // Return the full BitSet representing the tags
    }

    // Converts a BitSet back into a list of tags
    public static List<Tag> convertBitsToTags(BitSet bitSet) {
        List<Tag> tags = new ArrayList<>();
        int currentPosition = 0;

        while (currentPosition < bitSet.length()) {
            boolean isExtended = bitSet.get(currentPosition + Tag.OFFSET_LENGTH_BITS * 2); // Check if extended
            int charBitsLength = isExtended ? Tag.CHAR_24_BIT : Tag.CHAR_8_BIT; // Determine character bit length
            int tagBitLength = Tag.OFFSET_LENGTH_BITS * 2 + Tag.CHAR_FLAG_BITS + charBitsLength; // Total length of each tag

            if (currentPosition + tagBitLength <= bitSet.length()) { // Ensure it doesn't exceed the BitSet length
                BitSet tagBitSet = bitSet.get(currentPosition, currentPosition + tagBitLength); // Get the subset for the tag
                tags.add(Tag.fromBits(tagBitSet)); // Convert the bits to a tag
                currentPosition += tagBitLength; // Move to the next position
            } else {
                BitSet tagBitSet = bitSet.get(currentPosition, currentPosition + Tag.OFFSET_LENGTH_BITS * 2); // Handle edge case
                tags.add(Tag.fromBits(tagBitSet)); // Convert the subset to a tag
                currentPosition += Tag.OFFSET_LENGTH_BITS * 2; // Move forward to avoid out-of-bounds
                break;
            }
        }

        return tags; // Return the list of tags
    }

    // Decompresses the list of tags into the original text
    public static String decompressFromTags(List<Tag> tags) {
        StringBuilder decompressedText = new StringBuilder();

        for (Tag tag : tags) {
            int start = decompressedText.length() - tag.getOffset(); // Find the start of the match
            for (int i = 0; i < tag.getLength(); i++) {
                if (start + i < decompressedText.length()) {
                    decompressedText.append(decompressedText.charAt(start + i)); // Append the matching text
                }
            }
            if (tag.getNext() != '\0') {
                decompressedText.append(tag.getNext()); // Append the next character if it exists
            }
        }

        // Remove trailing newline characters
        while (decompressedText.length() > 0 &&
                (decompressedText.charAt(decompressedText.length() - 1) == '\n' ||
                 decompressedText.charAt(decompressedText.length() - 1) == '\r')) {
            decompressedText.deleteCharAt(decompressedText.length() - 1); // Remove newline at the end
        }

        return decompressedText.toString(); // Return the fully decompressed text
    }
}

// Node class for Huffman tree
class Node implements Comparable<Node> {
    int frequency; // Frequency of the node
    byte value; // Byte value of the node
    Node left; // Left child
    Node right; // Right child

    // Constructor for leaf nodes with a byte value
    public Node(int frequency, byte value) {
        this.frequency = frequency;
        this.value = value;
        this.left = null; // No children
        this.right = null; // No children
    }

    // Constructor for internal nodes with child nodes
    public Node(int frequency, Node left, Node right) {
        this.frequency = frequency;
        this.left = left;
        this.right = right;
    }

    // Check if the node is a leaf
    public boolean isLeaf() {
        return left == null && right == null; // Leaf nodes have no children
    }

    // Compare the frequency of nodes for priority queue ordering
    @Override
    public int compareTo(Node other) {
        return Integer.compare(this.frequency, other.frequency); // Order by frequency
    }
}

// Huffman class for Huffman coding
class Huffman {
    private Node root; // Root of the Huffman tree
    private Map<Byte, String> huffmanCodeMap = new HashMap<>(); // Huffman codes for each byte

    // Builds the Huffman tree from given data
    public void buildTree(byte[] data) {
        Map<Byte, Integer> frequencyMap = new HashMap<>(); // Frequency map for the bytes

        for (byte b : data) {
            frequencyMap.put(b, frequencyMap.getOrDefault(b, 0) + 1); // Increment frequency for each byte
        }

        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(n -> n.frequency)); // Priority queue for the nodes

        for (Map.Entry<Byte, Integer> entry : frequencyMap.entrySet()) {
            priorityQueue.add(new Node(entry.getValue(), entry.getKey())); // Add leaf nodes with frequencies
        }

        while (priorityQueue.size() > 1) { // While there are more than one node, merge the lowest frequency nodes
            Node left = priorityQueue.poll();
            Node right = priorityQueue.poll();
            Node newNode = new Node(left.frequency + right.frequency, left, right); // Create internal node
            priorityQueue.add(newNode); // Add the new node back to the queue
        }

        root = priorityQueue.poll(); // Get the root of the Huffman tree
        buildHuffmanCode(root, ""); // Build Huffman codes for the tree
    }

    // Builds Huffman codes from the tree recursively
    private void buildHuffmanCode(Node node, String code) {
        if (node.isLeaf()) {
            huffmanCodeMap.put(node.value, code); // Assign the code to the leaf node's value
        } else {
            buildHuffmanCode(node.left, code + "0"); // Left child has '0' appended to the code
            buildHuffmanCode(node.right, code + "1"); // Right child has '1' appended to the code
        }
    }

    // Compresses the given data using Huffman coding
    public byte[] compress(byte[] data) {
        buildTree(data); // Ensure the Huffman tree is built

        byte[] serializedTree = serializeTree(root); // Serialize the Huffman tree

        // Build the Huffman-encoded bit stream
        StringBuilder bitStream = new StringBuilder();
        for (byte b : data) {
            bitStream.append(huffmanCodeMap.get(b)); // Append the Huffman code for each byte
        }

        int numBits = bitStream.length(); // Get the total number of bits
        int numBytes = (int) Math.ceil(numBits / 8.0); // Calculate the number of bytes needed

        byte[] compressedData = new byte[serializedTree.length + 4 + numBytes]; // Array for the compressed data

        // Copy the serialized tree to the beginning of the compressed data
        System.arraycopy(serializedTree, 0, compressedData, 0, serializedTree.length);

        // Store the number of bits used in the bit stream for accurate decompression
        int offset = serializedTree.length; // Offset to start writing the bit stream
        compressedData[offset] = (byte) ((numBits >> 24) & 0xFF); // Store the high-order bits
        compressedData[offset + 1] = (byte) ((numBits >> 16) & 0xFF); // Store the second-order bits
        compressedData[offset + 2] = (byte) ((numBits >> 8) & 0xFF); // Store the third-order bits
        compressedData[offset + 3] = (byte) (numBits & 0xFF); // Store the low-order bits

        // Write the bit stream to the compressed data
        for (int i = 0; i < numBits; i++) {
            int byteIndex = offset + 4 + (i / 8); // Determine the byte index
            int bitIndex = 7 - (i % 8); // Determine the bit index within the byte
            if (bitStream.charAt(i) == '1') {
                compressedData[byteIndex] |= (1 << bitIndex); // Set the corresponding bit
            }
        }

        return compressedData; // Return the full compressed data
    }

    // Decompresses Huffman-encoded data
    public byte[] decompress(byte[] compressedData) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData); // Input stream for reading compressed data
        root = deserializeTree(inputStream); // Deserialize the Huffman tree

        // Read the number of bits used in the compressed data
        int numBits = ((inputStream.read() & 0xFF) << 24) |
                      ((inputStream.read() & 0xFF) << 16) |
                      ((inputStream.read() & 0xFF) << 8) |
                      (inputStream.read() & 0xFF);

        List<Byte> decompressedList = new ArrayList<>(); // List for storing decompressed bytes
        Node currentNode = root; // Start from the root of the Huffman tree

        int offset = 4 + serializeTree(root).length; // Correct offset calculation for compressed data

        // Traverse through the bit stream to decompress
        for (int i = 0; i < numBits; i++) {
            int byteIndex = offset + (i / 8); // Determine the byte index
            int bitIndex = 7 - (i % 8); // Determine the bit index
            boolean bit = (compressedData[byteIndex] & (1 << bitIndex)) != 0; // Check if the bit is set

            if (bit) {
                currentNode = currentNode.right; // Move to the right child for a '1' bit
            } else {
                currentNode = currentNode.left; // Move to the left child for a '0' bit
            }

            if (currentNode.isLeaf()) { // If it's a leaf, add the byte to the decompressed list
                decompressedList.add(currentNode.value);
                currentNode = root; // Reset to the root for the next traversal
            }
        }

        // Convert the list of bytes into a byte array for the final decompressed output
        byte[] decompressedData = new byte[decompressedList.size()];
        for (int i = 0; i < decompressedList.size(); i++) {
            decompressedData[i] = decompressedList.get(i); // Copy the bytes into the output array
        }

        return decompressedData; // Return the final decompressed data
    }

    // Serializes the Huffman tree into a byte array for storage
    private byte[] serializeTree(Node node) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); // Stream for storing the serialized tree
        serializeTreeHelper(node, byteStream); // Recursively serialize the tree
        return byteStream.toByteArray(); // Return the serialized tree as a byte array
    }

    // Recursive helper function to serialize the Huffman tree
    private void serializeTreeHelper(Node node, ByteArrayOutputStream byteStream) {
        if (node.isLeaf()) {
            byteStream.write(1); // Indicates a leaf node
            byteStream.write(node.value); // Store the leaf's byte value
        } else {
            byteStream.write(0); // Internal node indicator
            serializeTreeHelper(node.left, byteStream); // Serialize the left child
            serializeTreeHelper(node.right, byteStream); // Serialize the right child
        }
    }

    // Deserializes a Huffman tree from a byte array input stream
    private Node deserializeTree(ByteArrayInputStream inputStream) {
        int marker = inputStream.read(); // Read the marker to determine node type
        if (marker == 1) { // If it's a leaf
            byte value = (byte) inputStream.read(); // Read the leaf's byte value
            return new Node(0, value); // Return the leaf node
        } else { // If it's an internal node
            Node left = deserializeTree(inputStream); // Deserialize the left child
            Node right = deserializeTree(inputStream); // Deserialize the right child
            return new Node(0, left, right); // Return the internal node with children
        }
    }
}
