package com.example.caseconverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void swapAllAscii() {
        assertEquals("hELLO wORLD", Main.swapCase("Hello World"));
    }

    @Test
    void swapKeepsDigitsAndSymbols() {
        assertEquals("aBc 123 !@#", Main.swapCase("AbC 123 !@#"));
    }

    @Test
    void swapEmpty() {
        assertEquals("", Main.swapCase(""));
    }

    @Test
    void swapAllUpperToLower() {
        assertEquals("hello", Main.swapCase("HELLO"));
    }

    @Test
    void swapAllLowerToUpper() {
        assertEquals("HELLO", Main.swapCase("hello"));
    }
}
