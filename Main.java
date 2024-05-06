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
import java.util.Arrays;
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
                    System.out.print("Source file name: ");
                    sourceFile = sc.next().trim();
                    System.out.print("Archive name: ");
                    resultFile = sc.next().trim();
                    compress(sourceFile, resultFile);
                    break;

                case "decomp":
                    System.out.print("Archive name: ");
                    sourceFile = sc.next().trim();
                    System.out.print("Output file name: ");
                    resultFile = sc.next().trim();
                    decompress(sourceFile, resultFile);
                    break;

                case "size":
                    System.out.print("File name: ");
                    sourceFile = sc.next().trim();
                    fileSize(sourceFile);
                    break;

                case "equal":
                    System.out.print("First file name: ");
                    String firstFile = sc.next().trim();
                    System.out.print("Second file name: ");
                    String secondFile = sc.next().trim();
                    System.out.println(filesAreEqual(firstFile, secondFile) ? "Files are equal." : "Files are not equal.");
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
            // Read the text from the source file
            StringBuilder inputText = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(sourceFile), Charset.forName("UTF-8")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    inputText.append(line).append("\n");
                }
            }

            // LZ77 compression
            List<Tag> lz77Tags = LZ77.encode(inputText.toString());
            BitSet lz77BitSet = LZ77.convertTagsToBits(lz77Tags);
            byte[] lz77Bytes = lz77BitSet.toByteArray();

            // Huffman compression
            Huffman huffman = new Huffman();
            byte[] huffmanCompressed = huffman.compress(lz77Bytes);

            // Write the Huffman-compressed data to the output file
            writeBinaryFile(huffmanCompressed, resultFile);

            System.out.println("Compression complete.");
        } catch (IOException e) {
            System.err.println("Error during compression: " + e.getMessage());
        }
    }

    public static void decompress(String sourceFile, String resultFile) {
        try {
            byte[] compressedData = readBinaryFile(sourceFile);

            // Huffman decompression
            Huffman huffman = new Huffman();
            byte[] huffmanDecompressed = huffman.decompress(compressedData);

            // LZ77 decompression
            BitSet lz77BitSet = BitSet.valueOf(huffmanDecompressed);
            List<Tag> lz77Tags = LZ77.convertBitsToTags(lz77BitSet);
            String decompressedText = LZ77.decompressFromTags(lz77Tags);

            writeDecompressedToFile(decompressedText, resultFile);

            System.out.println("Decompression complete.");
        } catch (IOException e) {
            System.err.println("Error during decompression: " + e.getMessage());
        }
    }

    public static void fileSize(String sourceFile) {
        try {
            long size = Files.size(Paths.get(sourceFile));
            System.out.println("File size: " + size + " bytes");
        } catch (IOException e) {
            System.err.println("Error getting file size: " + e.getMessage());
        }
    }

    public static boolean filesAreEqual(String firstFile, String secondFile) {
        try {
            byte[] content1 = Files.readAllBytes(Paths.get(firstFile));
            byte[] content2 = Files.readAllBytes(Paths.get(secondFile));
            return Arrays.equals(content1, content2);
        } catch (IOException e) {
            System.err.println("Error comparing files: " + e.getMessage());
            return false;
        }
    }

    public static void about() {
        System.out.println("231RDB204 Daniels Stulpe 5.gupa");
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

    public static final int OFFSET_LENGTH_BITS = 16;
    public static final int CHAR_FLAG_BITS = 1;
    public static final int CHAR_8_BIT = 8;
    public static final int CHAR_24_BIT = 24;

    Tag(short offset, short length, char next, boolean isExtended) {
        this.offset = offset;
        this.length = length;
        this.next = next;
        this.isExtended = isExtended;
    }

    public short getOffset() {
        return offset;
    }

    public short getLength() {
        return length;
    }

    public char getNext() {
        return next;
    }

    public boolean isExtended() {
        return isExtended;
    }

    public BitSet convertToBits() {
        int charBitsLength = isExtended ? CHAR_24_BIT : CHAR_8_BIT;
        int totalBitsLength = OFFSET_LENGTH_BITS * 2 + CHAR_FLAG_BITS + charBitsLength;

        BitSet bitSet = new BitSet(totalBitsLength);

        // Shift for offset and length
        int index = 0;
        for (int i = 0; i < OFFSET_LENGTH_BITS; i++, index++) {
            bitSet.set(index, ((offset >> i) & 1) != 0);
        }
        for (int i = 0; i < OFFSET_LENGTH_BITS; i++, index++) {
            bitSet.set(index, ((length >> i) & 1) != 0);
        }

        // Flag for the character type (extended or not)
        bitSet.set(index++, isExtended);

        // Character (standard or extended)
        if (isExtended) {
            byte[] charBytes = Character.toString(next).getBytes(Charset.forName("UTF-8"));
            for (byte b : charBytes) {
                for (int i = 0; i < 8; i++, index++) {
                    bitSet.set(index, ((b >> i) & 1) != 0);
                }
            }
        } else {
            for (int i = 0; i < CHAR_8_BIT; i++, index++) {
                bitSet.set(index, ((next >> i) & 1) != 0);
            }
        }

        return bitSet;
    }

    public static Tag fromBits(BitSet bitSet) {
        int offset = 0;
        int length = 0;
        boolean isExtended = bitSet.get(OFFSET_LENGTH_BITS * 2); // Flag for character type

        // Read offset
        for (int i = 0; i < OFFSET_LENGTH_BITS; i++) {
            if (bitSet.get(i)) {
                offset |= (1 << i);
            }
        }

        // Read length
        for (int i = OFFSET_LENGTH_BITS; i < OFFSET_LENGTH_BITS * 2; i++) {
            if (bitSet.get(i)) {
                length |= (1 << (i - OFFSET_LENGTH_BITS));
            }
        }

        int charStart = OFFSET_LENGTH_BITS * 2 + CHAR_FLAG_BITS;
        char next = '\0';

        if (isExtended) {
            byte[] charBytes = new byte[3]; // 3 bytes for 24-bit character
            for (int i = 0; i < 3; i++) {
                byte charByte = 0;
                for (int j = 0; j < 8; j++) {
                    if (bitSet.get(charStart + (i * 8) + j)) {
                        charByte |= (1 << j);
                    }
                }
                charBytes[i] = charByte;
            }
            next = new String(charBytes, Charset.forName("UTF-8")).charAt(0);
        } else {
            byte simpleChar = 0;
            for (int i = 0; i < 8; i++) {
                if (bitSet.get(charStart + i)) {
                    simpleChar |= (1 << i);
                }
            }
            next = (char) simpleChar;
        }

        return new Tag((short) offset, (short) length, next, isExtended);
    }
}

// LZ77 class
class LZ77 {
    private static final int WINDOW_SIZE = 32767;
    private static final int MAX_LENGTH = 1000;

    public static List<Tag> encode(String text) {
        List<Tag> result = new ArrayList<>();
        int pos = 0;
        while (pos < text.length()) {
            Tag bestMatch = findMatching(text, pos, MAX_LENGTH);
            result.add(bestMatch);
            pos += bestMatch.getLength() + 1;
        }
        return result;
    }

    private static Tag findMatching(String text, int pos, int maxLength) {
        short offset = 0;
        short length = 0;
        char next = (pos < text.length()) ? text.charAt(pos) : '\0';

        int windowStart = Math.max(0, pos - WINDOW_SIZE);

        for (int i = pos - 1; i >= windowStart; i--) {
            int matchLength = 0;
            while (matchLength < maxLength && (pos + matchLength) < text.length() &&
                    text.charAt(i + matchLength) == text.charAt(pos + matchLength)) {
                matchLength++;
            }

            if (matchLength > length) {
                offset = (short) (pos - i);
                length = (short) matchLength;
                next = (pos + length < text.length()) ? text.charAt(pos + length) : '\0';
            }
        }

        boolean isExtended = (Character.toString(next).getBytes(Charset.forName("UTF-8")).length > 1);

        return new Tag(offset, length, next, isExtended);
    }

    public static BitSet convertTagsToBits(List<Tag> tags) {
        BitSet bitSet = new BitSet();
        int currentIndex = 0;

        for (Tag tag : tags) {
            int charBitsLength = tag.isExtended() ? Tag.CHAR_24_BIT : Tag.CHAR_8_BIT;
            int totalBitsLength = Tag.OFFSET_LENGTH_BITS * 2 + Tag.CHAR_FLAG_BITS + charBitsLength;

            BitSet tagBits = tag.convertToBits();
            for (int i = 0; i < totalBitsLength; i++) {
                bitSet.set(currentIndex + i, tagBits.get(i));
            }
            currentIndex += totalBitsLength;
        }

        return bitSet;
    }

    public static List<Tag> convertBitsToTags(BitSet bitSet) {
        List<Tag> tags = new ArrayList<>();
        int currentPosition = 0;

        while (currentPosition < bitSet.length()) {
            boolean isExtended = bitSet.get(currentPosition + Tag.OFFSET_LENGTH_BITS * 2);
            int charBitsLength = isExtended ? Tag.CHAR_24_BIT : Tag.CHAR_8_BIT;
            int tagBitLength = Tag.OFFSET_LENGTH_BITS * 2 + Tag.CHAR_FLAG_BITS + charBitsLength;

            if (currentPosition + tagBitLength <= bitSet.length()) {
                BitSet tagBitSet = bitSet.get(currentPosition, currentPosition + tagBitLength);
                tags.add(Tag.fromBits(tagBitSet));
                currentPosition += tagBitLength;
            } else {
                BitSet tagBitSet = bitSet.get(currentPosition, currentPosition + Tag.OFFSET_LENGTH_BITS * 2);
                tags.add(Tag.fromBits(tagBitSet));
                currentPosition += Tag.OFFSET_LENGTH_BITS * 2;
                break;
            }
        }
        return tags;
    }

    public static String decompressFromTags(List<Tag> tags) {
        StringBuilder decompressedText = new StringBuilder();

        for (Tag tag : tags) {
            int start = decompressedText.length() - tag.getOffset();
            for (int i = 0; i < tag.getLength(); i++) {
                if (start + i < decompressedText.length()) {
                    decompressedText.append(decompressedText.charAt(start + i));
                }
            }
            if (tag.getNext() != '\0') {
                decompressedText.append(tag.getNext());
            }
        }

        // Remove trailing newline characters from decompressed text
        while (decompressedText.length() > 0 &&
                (decompressedText.charAt(decompressedText.length() - 1) == '\n' ||
                        decompressedText.charAt(decompressedText.length() - 1) == '\r')) {
            decompressedText.deleteCharAt(decompressedText.length() - 1);
        }

        return decompressedText.toString();
    }
}


// Huffman class
class Node implements Comparable<Node> {
    int frequency;
    byte value;
    Node left;
    Node right;

    public Node(int frequency, byte value) {
        this.frequency = frequency;
        this.value = value;
        this.left = null;
        this.right = null;
    }

    public Node(int frequency, Node left, Node right) {
        this.frequency = frequency;
        this.left = left;
        this.right = right;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    @Override
    public int compareTo(Node other) {
        return Integer.compare(this.frequency, other.frequency);
    }
}

class Huffman {
    private Node root;
    private Map<Byte, String> huffmanCodeMap = new HashMap<>();

    public void buildTree(byte[] data) {
        Map<Byte, Integer> frequencyMap = new HashMap<>();
        for (byte b : data) {
            frequencyMap.put(b, frequencyMap.getOrDefault(b, 0) + 1);
        }

        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(n -> n.frequency));
        for (Map.Entry<Byte, Integer> entry : frequencyMap.entrySet()) {
            priorityQueue.add(new Node(entry.getValue(), entry.getKey()));
        }

        while (priorityQueue.size() > 1) {
            Node left = priorityQueue.poll();
            Node right = priorityQueue.poll();
            Node newNode = new Node(left.frequency + right.frequency, left, right);
            priorityQueue.add(newNode);
        }

        root = priorityQueue.poll();
        buildHuffmanCode(root, "");
    }

    private void buildHuffmanCode(Node node, String code) {
        if (node.isLeaf()) {
            huffmanCodeMap.put(node.value, code);
        } else {
            buildHuffmanCode(node.left, code + "0");
            buildHuffmanCode(node.right, code + "1");
        }
    }

    public byte[] compress(byte[] data) {
        buildTree(data); // Ensure the Huffman tree is built

        byte[] serializedTree = serializeTree(root);

        StringBuilder bitStream = new StringBuilder();
        for (byte b : data) {
            bitStream.append(huffmanCodeMap.get(b));
        }

        int numBits = bitStream.length();
        int numBytes = (int) Math.ceil(numBits / 8.0);

        byte[] compressedData = new byte[serializedTree.length + 4 + numBytes];

        // Copy the serialized tree to the beginning of the compressed data
        System.arraycopy(serializedTree, 0, compressedData, 0, serializedTree.length);

        // Store the number of bits used in the bit stream
        int offset = serializedTree.length;
        compressedData[offset] = (byte) ((numBits >> 24) & 0xFF);
        compressedData[offset + 1] = (byte) ((numBits >> 16) & 0xFF);
        compressedData[offset + 2] = (byte) ((numBits >> 8) & 0xFF);
        compressedData[offset + 3] = (byte) (numBits & 0xFF);

        // Write the bit stream to the compressed data
        for (int i = 0; i < numBits; i++) {
            int byteIndex = offset + 4 + (i / 8);
            int bitIndex = 7 - (i % 8);
            if (bitStream.charAt(i) == '1') {
                compressedData[byteIndex] |= (1 << bitIndex);
            }
        }

        return compressedData;
    }

    public byte[] decompress(byte[] compressedData) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);
        root = deserializeTree(inputStream);

        // Read the number of bits in the compressed data
        int numBits = ((inputStream.read() & 0xFF) << 24) |
                      ((inputStream.read() & 0xFF) << 16) |
                      ((inputStream.read() & 0xFF) << 8) |
                      (inputStream.read() & 0xFF);

        List<Byte> decompressedList = new ArrayList<>();
        Node currentNode = root;

        int offset = 4 + serializeTree(root).length; // Correct offset calculation

        for (int i = 0; i < numBits; i++) {
            int byteIndex = offset + (i / 8);
            int bitIndex = 7 - (i % 8);
            boolean bit = (compressedData[byteIndex] & (1 << bitIndex)) != 0;

            if (bit) {
                currentNode = currentNode.right;
            } else {
                currentNode = currentNode.left;
            }

            if (currentNode.isLeaf()) {
                decompressedList.add(currentNode.value);
                currentNode = root;
            }
        }

        // Convert the list of bytes into a byte array
        byte[] decompressedData = new byte[decompressedList.size()];
        for (int i = 0; i < decompressedList.size(); i++) {
            decompressedData[i] = decompressedList.get(i);
        }

        return decompressedData;
    }

    private byte[] serializeTree(Node node) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        serializeTreeHelper(node, byteStream);
        return byteStream.toByteArray();
    }

    private void serializeTreeHelper(Node node, ByteArrayOutputStream byteStream) {
        if (node.isLeaf()) {
            byteStream.write(1); // Indicates a leaf node
            byteStream.write(node.value); // Leaf's byte value
        } else {
            byteStream.write(0); // Internal node indicator
            serializeTreeHelper(node.left, byteStream);
            serializeTreeHelper(node.right, byteStream);
        }
    }

    private Node deserializeTree(ByteArrayInputStream inputStream) {
        int marker = inputStream.read();
        if (marker == 1) {
            byte value = (byte) inputStream.read();
            return new Node(0, value); // Leaf node
        } else {
            Node left = deserializeTree(inputStream);
            Node right = deserializeTree(inputStream);
            return new Node(0, left, right); // Internal node
        }
    }
}