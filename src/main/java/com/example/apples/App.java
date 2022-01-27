package com.example.apples;

import java.awt.Robot;
import java.io.File;

import javax.imageio.ImageIO;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Rectangle;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

public class App {

    static final int WIDTH = 17;
    static final int HEIGHT = 10;
    static final int TOP_CORNER_X = 638;
    static final int TOP_CORNER_Y = 233;
    static final int APPLE_SIZE = 73;
    // kinda arbitrary, 765 is max cuz 255 * 3 = 765
    static final int WHITE_THRESHOLD = 650;

    static int[][] apples;
    static int size = 2;

    public static void main(String[] args) throws Exception {
        getApples();
        int applesCleared = calculateMove();
        while (applesCleared > 0) {
            applesCleared = calculateMove();
        }
    }

    static void getApples() {
        apples = new int[HEIGHT][WIDTH];
        try {
            Robot bot = new Robot();
            // pause for 0.5s, switch tabs to apple game in this time
            bot.delay(500);
            // press reset and play buttons
            int mask = InputEvent.BUTTON1_DOWN_MASK;
            bot.mouseMove(650, 1050);
            bot.mousePress(mask);
            bot.mouseRelease(mask);
            bot.delay(100);
            bot.mouseMove(1000, 650);
            bot.mousePress(mask);
            bot.mouseRelease(mask);
            bot.delay(500);
            // take screenshot
            // rectangle is set for the bounds of the apple box
            BufferedImage img = bot.createScreenCapture(new Rectangle(TOP_CORNER_X, TOP_CORNER_Y, 1270, 772));
            ImageIO.write(img, "png", new File("./screenshot.png"));
            // binarize
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    final int clr = img.getRGB(x, y);
                    final int red = (clr & 0x00ff0000) >> 16;
                    final int green = (clr & 0x0000ff00) >> 8;
                    final int blue = clr & 0x000000ff;
                    // above white threshold and not green to white (cuz the apple numbers are
                    // red-ish)
                    if (red + green + blue > WHITE_THRESHOLD && green <= red && green <= blue) {
                        img.setRGB(x, y, new Color(255, 255, 255).getRGB());
                    } else {
                        img.setRGB(x, y, new Color(0, 0, 0).getRGB());
                    }
                }
            }
            ImageIO.write(img, "png", new File("./binarized.png"));

            // do OCR
            final ITesseract instance = new Tesseract();
            instance.setVariable("tessedit_char_whitelist", "0123456789");
            final String result = instance.doOCR(img);

            // convert to 2d array
            int row = 0;
            int col = 0;
            for (int i = 0; i < result.length(); i++) {
                char c = result.charAt(i);
                if (c == '\n') {
                    row++;
                    col = 0;
                } else if (c != ' ') {
                    // convert char to int
                    apples[row][col] = c - '0';
                    ++col;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void performClick(int row1, int col1, int row2, int col2) {
        try {
            // bot click
            Robot bot = new Robot();
            int mask = InputEvent.BUTTON1_DOWN_MASK;
            bot.mouseMove(TOP_CORNER_X + col1 * APPLE_SIZE, TOP_CORNER_Y + row1 * APPLE_SIZE);
            bot.mousePress(mask);
            // bot.delay(100);
            bot.mouseMove(TOP_CORNER_X + (col2 + 1) * APPLE_SIZE, TOP_CORNER_Y + (row2 + 1) * APPLE_SIZE);
            bot.mouseRelease(mask);
            // clear from data
            for (int i = 0; i <= row2 - row1; i++) {
                for (int j = 0; j <= col2 - col1; j++) {
                    apples[row1 + i][col1 + j] = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // returns the number of apples cleared
    static int calculateMove() {
        while (size < 11) {
            for (int row = 0; row < apples.length; row++) {
                for (int col = 0; col < apples[row].length; col++) {
                    int sum = apples[row][col];
                    // empty space
                    if (sum == 0) {
                        continue;
                    }
                    // go top-down
                    int k = 1;
                    while (row + k < apples.length && sum < 10 && k < size) {
                        sum += apples[row + k][col];
                        ++k;
                    }
                    if (sum == 10) {
                        --k;
                        performClick(row, col, row + k, col);
                        return k + 1;
                    }
                    // go across the row
                    sum = apples[row][col];
                    k = 1;
                    while (col + k < apples[row].length && sum < 10 && k < size) {
                        sum += apples[row][col + k];
                        k++;
                    }
                    if (sum == 10) {
                        --k;
                        performClick(row, col, row, col + k);
                        return k + 1;
                    }
                }
            }
            size++;
        }
        return 0;
    }
}
