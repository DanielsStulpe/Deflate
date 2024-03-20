import java.util.LinkedList;
import java.util.List;

public class LZ77 {

    public static List<Token> compress(String input) {
        List<Token> compressed = new LinkedList<>();
        int index = 0;
    
        while (index < input.length()) {
            int length = 0;
            int offset = 0;
            for (int i = 1; i <= Math.min(index, 255); i++) {
                int start = Math.max(index - i, 0); // Проверка на отрицательный старт
                String substr = input.substring(start, index); 
                int foundIndex = input.lastIndexOf(substr, index - 1);
                if (foundIndex != -1 && foundIndex < index && i > length) {
                    length = i;
                    offset = index - foundIndex;
                }
            }
    
            if (length == 0) {
                compressed.add(new Token(0, 0, input.charAt(index)));
                index++;
            } else {
                // Проверяем, что индекс символа в допустимом диапазоне строки
                int charIndex = index + length - 1;
                if (charIndex < input.length()) {
                    compressed.add(new Token(offset, length, input.charAt(charIndex)));
                } else {
                    // Если индекс выходит за пределы строки, добавляем последний символ строки
                    compressed.add(new Token(offset, length, input.charAt(input.length() - 1)));
                }
                index += length;
            }
        }
    
        return compressed;
    }

    public static String decompress(List<Token> compressed) {
        StringBuilder decompressed = new StringBuilder();

        for (Token token : compressed) {
            if (token.getLength() == 0) {
                decompressed.append(token.getCharacter());
            } else {
                int startIndex = decompressed.length() - token.getOffset();
                for (int i = 0; i < token.getLength(); i++) {
                    decompressed.append(decompressed.charAt(startIndex + i));
                }
                decompressed.append(token.getCharacter());
            }
        }

        return decompressed.toString();
    }

    public static void main(String[] args) {
        String input = "Strings are immutable in Java, which means we cannot change a String character encoding.";
        List<Token> compressed = compress(input);
        System.out.println("Compressed: " + compressed);
        String decompressed = decompress(compressed);
        System.out.println("Decompressed: " + decompressed);
    }
}

class Token {
    private int offset;
    private int length;
    private char character;

    public Token(int offset, int length, char character) {
        this.offset = offset;
        this.length = length;
        this.character = character;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public char getCharacter() {
        return character;
    }

    @Override
    public String toString() {
        return "(" + offset + "," + length + ",'" + character + "')";
    }
}
