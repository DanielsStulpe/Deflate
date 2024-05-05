import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Scanner;

// Класс Tag
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

        // Сдвиг offset и length
        int index = 0;
        for (int i = 0; i < OFFSET_LENGTH_BITS; i++, index++) {
            bitSet.set(index, ((offset >> i) & 1) != 0);
        }
        for (int i = 0; i < OFFSET_LENGTH_BITS; i++, index++) {
            bitSet.set(index, ((length >> i) & 1) != 0);
        }

        // Флаг для символа (extended или нет)
        bitSet.set(index++, isExtended);

        // Символ (обычный или расширенный)
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
        boolean isExtended = bitSet.get(Tag.OFFSET_LENGTH_BITS * 2); // Флаг для определения типа символа

        // Получаем offset
        for (int i = 0; i < Tag.OFFSET_LENGTH_BITS; i++) {
            if (bitSet.get(i)) {
                offset |= (1 << i);
            }
        }

        // Получаем length
        for (int i = Tag.OFFSET_LENGTH_BITS; i < Tag.OFFSET_LENGTH_BITS * 2; i++) {
            if (bitSet.get(i)) {
                length |= (1 << (i - Tag.OFFSET_LENGTH_BITS));
            }
        }

        int charStart = Tag.OFFSET_LENGTH_BITS * 2 + Tag.CHAR_FLAG_BITS;
        char next = '\0';

        if (isExtended) {
            byte[] charBytes = new byte[3]; // 3 байта для 24-битного символа
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

// Класс LZ77
class LZ77 {
    private static final int WINDOW_SIZE = 32000;
    private static final int MAX_LENGTH = 500;

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
                break; // Прерываем цикл, чтобы избежать выхода за пределы
            }
        }

        return tags;
        }

    public static void writeEncodedToFile(List<Tag> tags, String filename) {
        BitSet encodedBits = convertTagsToBits(tags);
        byte[] compressedBytes = encodedBits.toByteArray();

        try {
            Files.write(Paths.get(filename), compressedBytes);
            System.out.println("Сжатый текст успешно записан в файл " + filename);
        } catch (IOException ex) {
            System.err.println("Ошибка при записи в файл: " + ex.getMessage());
        }
    }

    public static String decompressFromFile(String filename) {
        try {
            byte[] compressedBytes = Files.readAllBytes(Paths.get(filename));
            if (compressedBytes.length == 0) {
                System.err.println("Ошибка: файл пустой.");
                return "";
            }

            BitSet tagsBitSet = BitSet.valueOf(compressedBytes);
            List<Tag> tags = convertBitsToTags(tagsBitSet);

            if (tags.isEmpty()) {
                System.err.println("Ошибка: не удалось получить теги из битов.");
                return "";
            }

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

            return decompressedText.toString();

        } catch (IOException ex) {
            System.err.println("Ошибка при чтении файла: " + ex.getMessage());
            return "";
        }
    }
}

// Класс Main
public class Main {
    public static void compress(String sourceFile, String archiveFile) {
        try {
            StringBuilder inputText = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(sourceFile), Charset.forName("UTF-8")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    inputText.append(line).append("\n");
                }
            }

            List<Tag> encoded = LZ77.encode(inputText.toString());
            LZ77.writeEncodedToFile(encoded, archiveFile);

            System.out.println("Файл успешно сжат и сохранен в архив " + archiveFile);
        } catch (IOException ex) {
            System.err.println("Ошибка при сжатии: " + ex.getMessage());
        }
    }

    public static void decompress(String sourceFile, String outputFile) {
        try {
            String decompressedText = LZ77.decompressFromFile(sourceFile);

            if (!decompressedText.isEmpty()) {
                try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8")))) {
                    writer.write(decompressedText);
                }

                System.out.println("Файл успешно декомпрессирован и сохранен в " + outputFile);
            } else {
                System.err.println("Ошибка: декомпрессированный текст пуст.");
            }
        } catch (IOException ex) {
            System.err.println("Ошибка при декомпрессии: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            String sourceFile, resultFile;

            System.out.println("Выберите действие:");
            System.out.println("(comp для сжатия, decomp для декомпрессии): ");
            String choiceStr = sc.next();

            switch (choiceStr) {
                case "comp":
                    System.out.print("Имя исходного файла для сжатия: ");
                    sourceFile = sc.next();
                    System.out.print("Имя архива: ");
                    resultFile = sc.next();
                    compress(sourceFile, resultFile);
                    break;

                case "decomp":
                    System.out.print("Имя архива: ");
                    sourceFile = sc.next();
                    System.out.print("Имя файла для сохранения: ");
                    resultFile = sc.next();
                    decompress(sourceFile, resultFile);
                    break;

                default:
                    System.err.println("Неверный выбор.");
            }
        } catch (Exception ex) {
            System.err.println("Произошла ошибка: " + ex.getMessage());
        }
    }
}
