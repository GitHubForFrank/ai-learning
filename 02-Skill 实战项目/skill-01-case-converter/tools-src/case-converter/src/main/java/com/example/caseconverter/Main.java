package com.example.caseconverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(2);
            return;
        }

        String mode = args[0];
        String text;
        if (args.length >= 2) {
            text = args[1];
        } else {
            try {
                text = readAllStdin();
            } catch (IOException e) {
                System.err.println("读取 stdin 失败: " + e.getMessage());
                System.exit(1);
                return;
            }
        }

        String result;
        switch (mode) {
            case "upper":
                result = text.toUpperCase();
                break;
            case "lower":
                result = text.toLowerCase();
                break;
            case "swap":
                result = swapCase(text);
                break;
            default:
                System.err.println("未知 mode: " + mode);
                printUsage();
                System.exit(2);
                return;
        }

        System.out.println(result);
    }

    static String swapCase(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(Character.toLowerCase(c));
            } else if (Character.isLowerCase(c)) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String readAllStdin() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (!first) {
                    sb.append('\n');
                }
                sb.append(line);
                first = false;
            }
        }
        return sb.toString();
    }

    private static void printUsage() {
        System.err.println("用法: java -jar case-converter.jar <mode> [text]");
        System.err.println("  mode: upper | lower | swap");
        System.err.println("  text: 待转换字符串；省略则从 stdin 读取");
    }
}
